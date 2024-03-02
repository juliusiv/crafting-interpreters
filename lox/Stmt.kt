package com.craftinginterpreters.lox;

import kotlin.collections.List;

interface Stmt {
  interface Visitor<out R> {
    fun visitExpressionStmt(stmt: Stmt.Expression): R;
    fun visitPrintStmt(stmt: Stmt.Print): R;
  }

  fun <R> accept(visitor: Visitor<R>): R;

  class Expression(val expression: Expr) : Stmt {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitExpressionStmt(this);
    }
  }

  class Print(val expression: Expr) : Stmt {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitPrintStmt(this);
    }
  }

}

