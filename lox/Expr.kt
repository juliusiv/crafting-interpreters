package com.craftinginterpreters.lox;

import kotlin.collections.List;

interface Expr {
  interface Visitor<out R> {
    fun visitAssignExpr(expr: Expr.Assign): R;
    fun visitBinaryExpr(expr: Expr.Binary): R;
    fun visitGroupingExpr(expr: Expr.Grouping): R;
    fun visitLiteralExpr(expr: Expr.Literal): R;
    fun visitLogicalExpr(expr: Expr.Logical): R;
    fun visitUnaryExpr(expr: Expr.Unary): R;
    fun visitVariableExpr(expr: Expr.Variable): R;
  }

  fun <R> accept(visitor: Visitor<R>): R;

  class Assign(val name: Token, val  value: Expr) : Expr {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitAssignExpr(this);
    }
  }

  class Binary(val left: Expr, val  operator: Token, val  right: Expr) : Expr {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitBinaryExpr(this);
    }
  }

  class Grouping(val expression: Expr) : Expr {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitGroupingExpr(this);
    }
  }

  class Literal(val value: Any?) : Expr {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitLiteralExpr(this);
    }
  }

  class Logical(val left: Expr, val  operator: Token, val  right: Expr) : Expr {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitLogicalExpr(this);
    }
  }

  class Unary(val operator: Token, val  right: Expr) : Expr {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitUnaryExpr(this);
    }
  }

  class Variable(val name: Token) : Expr {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitVariableExpr(this);
    }
  }

}

