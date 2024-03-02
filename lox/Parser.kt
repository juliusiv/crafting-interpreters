package com.craftinginterpreters.lox;

import kotlin.collections.List;

import com.craftinginterpreters.lox.TokenType.*;
// import com.craftinginterpreters.lox.Lox;

/**
 * Expression Grammar
 * 
 * expression    → equality ;
 * equality      → comparison ( ( "!=" | "==" ) comparison )* ;
 * comparison    → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 * term          → factor ( ( "-" | "+" ) factor )* ;
 * factor        → unary ( ( "/" | "*" ) unary )* ;
 * unary         → ( "!" | "-" ) unary
 *               | primary ;
 * primary       → NUMBER | STRING | "true" | "false" | "nil"
 *               | "(" expression ")" ;
 */

class Parser(val tokens: List<Token>) {
  class ParseError(message: String) : RuntimeException(message);

  var current = 0;

  fun parse(): Expr? {
    try {
      return expression();
    } catch (error: ParseError) {
      return null;
    }
  }

  private fun expression(): Expr {
    return equality();
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