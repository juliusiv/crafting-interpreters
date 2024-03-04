package com.craftinginterpreters.lox;

class Environment(val enclosing: Environment? = null) {
  private final val values: MutableMap<String, Any?> = mutableMapOf();

  fun define(name: String, value: Any?) {
    values.put(name, value);
  }

  fun get(name: Token): Any? {
    if (name.lexeme in values) {
      return values.get(name.lexeme);
    }

    if (enclosing != null) return enclosing.get(name);

    throw RuntimeError(name, "Undefined variable '${name.lexeme}'.");
  }

  fun assign(name: Token, value: Any?) {
    if (name.lexeme in values) {
      values.put(name.lexeme, value);
      return;
    }

    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }

    throw RuntimeError(name, "Undefined variable '${name.lexeme}'.");
  }
}