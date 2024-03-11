package com.craftinginterpreters.lox;

class AstPrinter : Expr.Visitor<String> {
  fun print(expr: Expr): String {
    return expr.accept(this);
  }

  override fun visitAssignExpr(expr: Expr.Assign): String {
    return parenthesize(expr.name.lexeme, expr.value);
  }


  override fun visitBinaryExpr(expr: Expr.Binary): String {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  override fun visitCallExpr(expr: Expr.Call): String {
    return "nice"
  }

  override fun visitGroupingExpr(expr: Expr.Grouping ): String {
    return parenthesize("group", expr.expression);
  }

  override fun visitLiteralExpr(expr: Expr.Literal ): String {
    if (expr.value == null) return "nil";
    return expr.value.toString();
  }

  override fun visitLogicalExpr(expr: Expr.Logical): String {
    return "stuff"
  }

  override fun visitUnaryExpr(expr: Expr.Unary): String {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  override fun visitVariableExpr(expr: Expr.Variable): String {
    return expr.name.lexeme;
  }

  fun parenthesize(name: String, vararg exprs: Expr): String {
    val builder = StringBuilder();
  
    builder.append("(").append(name);
    for (expr in exprs) {
      builder.append(" ");
      builder.append(expr.accept(this));
    }
    builder.append(")");
  
    return builder.toString();
  }
}

// This won't exhaustively match all the subtypes of Expr so it's not really what I want :(
// fun printAst(expr: Expr): String {
//   return when(expr) {
//     is Binary -> ""
//     // is Binary -> parenthesize(expr.operator.lexeme, expr.left, expr.right)
//     is Grouping -> ""
//     is Literal -> ""
//     is Unary -> ""
//   }
// }