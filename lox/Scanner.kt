package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.TokenType;

val KEYWORDS = hashMapOf(
  "and" to TokenType.AND,
  "class" to TokenType.CLASS,
  "else"  to TokenType.ELSE,
  "false" to TokenType.FALSE,
  "for"   to TokenType.FOR,
  "fun"   to TokenType.FUN,
  "if"    to TokenType.IF,
  "nil"   to TokenType.NIL,
  "or"    to TokenType.OR,
  "print" to TokenType.PRINT,
  "return" to TokenType.RETURN,
  "super" to TokenType.SUPER,
  "this"  to TokenType.THIS,
  "true"  to TokenType.TRUE,
  "var"   to TokenType.VAR,
  "while" to TokenType.WHILE,
)

class Scanner(
  val source: String,
  val tokens: ArrayList<Token> = arrayListOf()
) {
  private var start = 0
  private var current = 0
  private var line = 1

  fun scanTokens(): List<Token> {
    while (!isAtEnd()) {
      // We are at the beginning of the next lexeme.
      start = current;
      scanToken();
    }

    tokens.add(Token(TokenType.EOF, "", null, line));
    return tokens;
  }

  private fun scanToken() {
    val c: Char = advance();

    when (c) {
      '(' -> addToken(TokenType.LEFT_PAREN)
      ')' -> addToken(TokenType.RIGHT_PAREN)
      '{' -> addToken(TokenType.LEFT_BRACE)
      '}' -> addToken(TokenType.RIGHT_BRACE)
      ',' -> addToken(TokenType.COMMA)
      '.' -> addToken(TokenType.DOT)
      '-' -> addToken(TokenType.MINUS)
      '+' -> addToken(TokenType.PLUS)
      ';' -> addToken(TokenType.SEMICOLON)
      '*' -> addToken(TokenType.STAR)
      '!' -> addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
      '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
      '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
      '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
      '/' ->
        if (match('/')) {
          // A comment goes until the end of the line.
          while (peek() != '\n' && !isAtEnd()) advance();
        } else {
          addToken(TokenType.SLASH);
        }
      ' ', '\r', '\t' -> Unit
      '\n' -> line++
      // Using '"' for the double quote char seems to break the VSCode syntax highlighting so use a Char object
      Char(34) -> string()
      // in '0'..'9' -> number() // would also work for matching numbers but leaving it in the default case to match the book
      else -> {
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) {
          identifier()
        } else {
          error(line, "Unexpected character.");
        }
        // error(line, "Unexpected character.");
      }
    }
  }

  private fun identifier() {
    while (isAlphaNumeric(peek())) advance()

    val text = source.substring(start, current)
    val type = KEYWORDS.get(text) ?: TokenType.IDENTIFIER

    addToken(type);
  }

  private fun number() {
    while (isDigit(peek())) advance();

    // Look for a fractional part.
    if (peek() == '.' && isDigit(peekNext())) {
      // Consume the "."
      advance();

      while (isDigit(peek())) advance();
    }

    addToken(TokenType.NUMBER, source.substring(start, current).toDouble());
  }

  private fun string() {
    // Using '"' for the double quote char seems to break the VSCode syntax highlighting so use a Char object
    while (peek() != Char(34) && !isAtEnd()) {
      if (peek() == '\n') line++;
      advance();
    }

    if (isAtEnd()) {
      error(line, "Unterminated string.");
      return;
    }

    // The closing ".
    advance();

    // Trim the surrounding quotes.
    val value = source.substring(start + 1, current - 1);
    addToken(TokenType.STRING, value);
  }

  private fun match(expected: Char): Boolean {
    if (isAtEnd()) return false;
    if (source.elementAt(current) != expected) return false;

    current++;
    return true;
  }

  private fun peek(): Char {
    if (isAtEnd()) return Char(0);
    return source.elementAt(current);
  }

  private fun peekNext(): Char {
    if (current + 1 >= source.length) return Char(0);
    return source.elementAt(current + 1);
  }

  private fun isAlpha(c: Char): Boolean {
    return (c in 'a'..'z') ||
           (c in 'A'..'Z') ||
            c == '_';
  }

  private fun isAlphaNumeric(c: Char): Boolean {
    return isAlpha(c) || isDigit(c);
  }

  private fun isDigit(c: Char): Boolean {
    return c in '0'..'9'
  } 

  private fun isAtEnd(): Boolean {
    return current >= source.length
  }

  private fun advance(): Char {
    return source.elementAt(current++)
  }

  private fun addToken(type: TokenType) {
    addToken(type, null);
  }

  private fun addToken(type: TokenType, literal: Any?) {
    val text = source.substring(start, current);
    tokens.add(Token(type, text, literal, line));
  }
}