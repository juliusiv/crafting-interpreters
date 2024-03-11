package com.craftinginterpreters.lox;

class Environment(val enclosing: Environment? = null) {
  private final val values: MutableMap<String, Any?> = mutableMapOf();

  fun define(name: String, value: Any?) {
    values.put(name, value);
  }

  fun getAt(distance: Int, name: String): Any? {
    return ancestor(distance)?.values?.get(name);
  }

  fun assignAt(distance: Int,name: Token, value: Any?) {
    ancestor(distance)?.values?.put(name.lexeme, value);
  }

  fun ancestor(distance: Int): Environment? {
    var environment: Environment? = this;
    for (i in 0..distance - 1) {
      environment = environment?.enclosing; 
    }

    return environment;
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