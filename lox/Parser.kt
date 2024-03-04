package com.craftinginterpreters.lox;

import kotlin.collections.List;

import com.craftinginterpreters.lox.TokenType.*;

/**
 * Expression Grammar
   program        → declaration* EOF ;
   declaration    → varDecl
                  | statement ;
   varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
   statement      → exprStmt
                  | printStmt ;
   exprStmt       → expression ";" ;
   printStmt      → "print" expression ";" ;
   statement      → exprStmt
                  | printStmt
                  | block ;
   block          → "{" declaration* "}" ;
   expression     → assignment ;
   assignment     → IDENTIFIER "=" assignment
                  | equality ;
   equality       → comparison ( ( "!=" | "==" ) comparison )* ;
   comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
   term           → factor ( ( "-" | "+" ) factor )* ;
   factor         → unary ( ( "/" | "*" ) unary )* ;
   unary          → ( "!" | "-" ) unary
                  | primary ;
   primary        → NUMBER | STRING | "true" | "false" | "nil"
                  | "(" expression ")"
                  | IDENTIFIER ;
 */

class Parser(val tokens: List<Token>) {
  class ParseError(message: String) : RuntimeException(message);

  var current = 0;

  fun parse(): List<Stmt> {
    val statements: MutableList<Stmt> = ArrayList<Stmt>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }

    return statements.toList();
  }

  private fun expression(): Expr {
    return assignment();
  }

  private fun declaration(): Stmt {
    if (match(VAR)) return varDeclaration();
    return statement();
    // TODO: the null return seems weird... instead of doing Stmt? everywhere, let's see if this gets addressed
    // try {
    //   if (match(VAR)) return varDeclaration();

    //   return statement();
    // } catch (ParseError error) {
    //   synchronize();

    //   return null;
    // }
  }

  private fun statement(): Stmt {
    if (match(PRINT)) return printStatement();
    if (match(LEFT_BRACE)) return Stmt.Block(block());

    return expressionStatement();
  }

  private fun printStatement(): Stmt {
    val value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return Stmt.Print(value);
  }

  private fun varDeclaration(): Stmt {
    val name = consume(IDENTIFIER, "Expect variable name.");

    var initializer: Expr? = null;
    if (match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Expect ';' after variable declaration.");
    return Stmt.Var(name, initializer);
  }

  private fun expressionStatement(): Stmt {
    val expr = expression();
    consume(SEMICOLON, "Expect ';' after expression.");
    return Stmt.Expression(expr);
  }

  private fun block(): List<Stmt> {
    val statements = mutableListOf<Stmt>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Expect '}' after block.");
    return statements;
  }

  private fun assignment(): Expr {
    val expr = equality();

    if (match(EQUAL)) {
      val equals = previous();
      val value = assignment();

      if (expr is Expr.Variable) {
        val name = expr.name;
        return Expr.Assign(name, value);
      }

      error(equals, "Invalid assignment target."); 
    }

    return expr;
  }

  private fun equality(): Expr {
    var expr = comparison();

    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      val operator = previous();
      val right = comparison();
      expr = Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private fun comparison(): Expr {
    var expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      val operator = previous();
      val right = term();
      expr = Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private fun term(): Expr {
    var expr = factor();

    while (match(MINUS, PLUS)) {
      val operator = previous();
      val right = factor();
      expr = Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private fun factor(): Expr {
    var expr = unary();

    while (match(SLASH, STAR)) {
      val operator = previous();
      val right = unary();
      expr = Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private fun unary(): Expr {
    if (match(BANG, MINUS)) {
      val operator = previous();
      val right = unary();
      return Expr.Unary(operator, right);
    }

    return primary();
  }

  @Throws(ParseError::class)
  private fun primary(): Expr {
    if (match(FALSE)) return Expr.Literal(false);
    if (match(TRUE)) return Expr.Literal(true);
    if (match(NIL)) return Expr.Literal(null);

    if (match(NUMBER, STRING)) {
      return Expr.Literal(previous().literal);
    }

    if (match(IDENTIFIER)) {
      return Expr.Variable(previous());
    }

    if (match(LEFT_PAREN)) {
      val expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
  }

  // Checks to see if the current token has any of the given types. If so, it consumes the token
  // and returns true. Otherwise, it returns false and leaves the current token alone.
  private fun match(vararg types: TokenType): Boolean {
    for (type in types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  @Throws(ParseError::class)
  private fun consume(type: TokenType, message: String): Token {
    if (check(type)) return advance();

    throw error(peek(), message);
  }

  // Returns true if the current token is of the given type.
  private fun check(type: TokenType): Boolean {
    if (isAtEnd()) return false;
    return peek().type == type;
  }

  // Consumes the current token and returns it, similar to how our scanner’s corresponding method
  // crawled through characters.
  private fun advance(): Token {
    if (!isAtEnd()) current++;
    return previous();
  }

  // Checks if we’ve run out of tokens to parse.
  private fun isAtEnd(): Boolean {
    return peek().type == EOF;
  }

  // Returns the current token we have yet to consume.
  private fun peek(): Token {
    return tokens.get(current);
  }

  // Returns the most recently consumed token
  private fun previous(): Token {
    return tokens.get(current - 1);
  }

  private fun error(token: Token, message: String): ParseError {
    loxError(token, message);
    return ParseError(message);
  }

  // Synchronize the parser after an error is found so they don't cascade to the end of the file.
  private fun synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) return;

      when (peek().type) {
        CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return
        else -> Unit
      }

      advance();
    }
  }
}