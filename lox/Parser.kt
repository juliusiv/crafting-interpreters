package com.craftinginterpreters.lox;

import kotlin.collections.List;

import com.craftinginterpreters.lox.TokenType.*;
import java.util.Arrays

/**
 * Expression Grammar
   program        → declaration* EOF ;
   declaration    → funDecl
                  | varDecl
                  | statement ;
   funDecl        → "fun" function ;
   function       → IDENTIFIER "(" parameters? ")" block ;
   parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
   varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
   statement      → exprStmt
                  | printStmt ;
   exprStmt       → expression ";" ;
   printStmt      → "print" expression ";" ;
   statement      → exprStmt
                  | forStmt
                  | ifStmt
                  | printStmt
                  | returnStmt
                  | whileStmt
                  | block ;
   returnStmt     → "return" expression? ";" ;
   forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
                    expression? ";"
                    expression? ")" statement ;
   whileStmt      → "while" "(" expression ")" statement ;
   ifStmt         → "if" "(" expression ")" statement ( "else" statement )? ;
   block          → "{" declaration* "}" ;
   expression     → assignment ;
   assignment     → IDENTIFIER "=" assignment
                  | logic_or ;
   logic_or       → logic_and ( "or" logic_and )* ;
   logic_and      → equality ( "and" equality )* ;  
   equality       → comparison ( ( "!=" | "==" ) comparison )* ;
   comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
   term           → factor ( ( "-" | "+" ) factor )* ;
   factor         → unary ( ( "/" | "*" ) unary )* ;
   unary          → ( "!" | "-" ) unary | call;
   arguments      → expression ( "," expression )* ;
   call           → primary ( "(" arguments? ")" )* ;
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
    if (match(FUN)) return function("function");
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
    if (match(FOR)) return forStatement();
    if (match(IF)) return ifStatement();
    if (match(PRINT)) return printStatement();
    if (match(RETURN)) return returnStatement();
    if (match(WHILE)) return whileStatement();
    if (match(LEFT_BRACE)) return Stmt.Block(block());

    return expressionStatement();
  }

  private fun forStatement(): Stmt {
    consume(LEFT_PAREN, "Expect '(' after 'for'.");

    val initializer = if (match(SEMICOLON)) {
      null;
    } else if (match(VAR)) {
      varDeclaration();
    } else {
      expressionStatement();
    }

    var condition = if (!check(SEMICOLON)) expression() else null;
    consume(SEMICOLON, "Expect ';' after loop condition.");

    val increment = if (!check(RIGHT_PAREN)) expression() else null;
    consume(RIGHT_PAREN, "Expect ')' after for clauses.");

    var body = statement();

    if (increment != null) {
      body = Stmt.Block(Arrays.asList(body, Stmt.Expression(increment)));
    }

    if (condition == null) condition = Expr.Literal(true);
    body = Stmt.While(condition, body);

    if (initializer != null) {
      body = Stmt.Block(Arrays.asList(initializer, body));
    }

    return body;
  }

  private fun whileStatement(): Stmt {
    consume(LEFT_PAREN, "Expect '(' after 'while'.");
    val condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after condition.");
    val body = statement();

    return Stmt.While(condition, body);
  }

  private fun ifStatement(): Stmt {
    consume(LEFT_PAREN, "Expect '(' after 'if'.");
    val condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after if condition."); 

    val thenBranch = statement();
    var elseBranch: Stmt? = null;
    if (match(ELSE)) {
      elseBranch = statement();
    }

    return Stmt.If(condition, thenBranch, elseBranch);
  }

  private fun printStatement(): Stmt {
    val value = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return Stmt.Print(value);
  }

  private fun returnStatement(): Stmt {
    val keyword = previous();
    // TODO: hmmm
    // var value: Expr? = null;
    if (check(SEMICOLON)) {
      return Stmt.Return(keyword, null)
    }
    
    val value = expression();
    // if (!check(SEMICOLON)) {
    //   value = expression();
    // }

    consume(SEMICOLON, "Expect ';' after return value.");
    return Stmt.Return(keyword, value);
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

  private fun function(kind: String): Stmt.Function {
    val name = consume(IDENTIFIER, "Expect $kind name.");
    consume(LEFT_PAREN, "Expect '(' after $kind name.");

    val parameters = ArrayList<Token>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (parameters.size >= 255) {
          error(peek(), "Can't have more than 255 parameters.");
        }

        parameters.add(
            consume(IDENTIFIER, "Expect parameter name."));
      } while (match(COMMA));
    }
    consume(RIGHT_PAREN, "Expect ')' after parameters.");

    consume(LEFT_BRACE, "Expect '{' before $kind body.");
    val body = block();

    return Stmt.Function(name, parameters, body);
  }

  private fun assignment(): Expr {
    val expr = or();

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

  private fun or(): Expr {
    var expr = and();

    while (match(OR)) {
      val operator = previous();
      val right = and();
      expr = Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private fun and(): Expr {
    var expr = equality();

    while (match(AND)) {
      val operator = previous();
      val right = equality();
      expr = Expr.Logical(expr, operator, right);
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

    return call();
  }

  private fun call(): Expr {
    var expr = primary();

    while (true) { 
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
      } else {
        break;
      }
    }

    return expr;
  }

  private fun finishCall(callee: Expr): Expr {
    val arguments = ArrayList<Expr>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (arguments.size >= 255) {
          error(peek(), "Can't have more than 255 arguments.");
        }

        arguments.add(expression());
      } while (match(COMMA));
    }

    val paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

    return Expr.Call(callee, paren, arguments);
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