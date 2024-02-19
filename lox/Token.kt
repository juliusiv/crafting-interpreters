package com.craftinginterpreters.lox

import com.craftinginterpreters.lox.TokenType

// Make a data class?
class Token(
  val type: TokenType,
  val lexeme: String,
  val literal: Any?, // instead of Object
  val line: Int
) {

  override fun toString(): String {
    return type.toString() + " " + lexeme + " " + literal;
  }
}