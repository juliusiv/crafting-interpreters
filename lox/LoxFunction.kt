package com.craftinginterpreters.lox;

import kotlin.collections.List;

class LoxFunction(final val declaration: Stmt.Function, final val closure: Environment) : LoxCallable {
  override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
    val environment = Environment(closure);

    for (i in declaration.params.indices) {
      environment.define(
        declaration.params.get(i).lexeme,
        arguments.get(i)
      );
    }

    try {
      interpreter.executeBlock(declaration.body, environment);
    } catch (returnValue: Return) {
      return returnValue.value;
    }

    return null;
  }

  override fun arity(): Int {
    return declaration.params.size;
  }

  override fun toString(): String {
    return "<fn ${declaration.name.lexeme}>";
  }
}