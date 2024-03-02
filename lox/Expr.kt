package com.craftinginterpreters.lox;

import kotlin.collections.List;

interface Visitor<out R> {
  fun visitBinaryExpr(expr: Expr.Binary): R;
  fun visitGroupingExpr(expr: Expr.Grouping): R;
  fun visitLiteralExpr(expr: Expr.Literal): R;
  fun visitUnaryExpr(expr: Expr.Unary): R;
}

interface Expr {
  fun <R> accept(visitor: Visitor<R>): R;

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
  
  class Unary(val operator: Token, val  right: Expr) : Expr {
    override fun <R> accept(visitor: Visitor<R>): R {
      return visitor.visitUnaryExpr(this);
    }
  }
}


