package com.craftinginterpreters.lox;

import kotlin.collections.List;

interface LoxCallable {
  fun arity(): Int;

  fun call(interpreter: Interpreter, arguments: List<Any?>): Any?;
}