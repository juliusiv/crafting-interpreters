package com.craftinginterpreters.lox;

import kotlin.collections.List;

interface Stmt {
  interface Visitor<out R> {
    fun visitBlockStmt(stmt: Stmt.Block): R;
    fun visitExpressionStmt(stmt: Stmt.Expression): R;
    fun visitPrintStmt(stmt: Stmt.Print): R;
    fun visitVarStmt(stmt: Stmt.Var): R;
  }

  fun <R> accept(visitor: Visitor<R>): R;

  class Block(val statements: List<Stmt>) : Stmt {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitBlockStmt(this);
    }
  }

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

  class Var(val name: Token, val  initializer: Expr?) : Stmt {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitVarStmt(this);
    }
  }

}

