package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.TokenType.*;
import com.craftinginterpreters.lox.RuntimeError

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
  fun interpret(statements: List<Stmt>) {
    try {
      for (statement in statements) {
        execute(statement);
      }
    } catch (error: RuntimeError) {
      loxRuntimeError(error);
    }
  }

  override fun visitLiteralExpr(expr: Expr.Literal): Any? {
    return expr.value;
  }

  override fun visitUnaryExpr(expr: Expr.Unary): Any? {
    val right = evaluate(expr.right);

    return when (expr.operator.type) {
      BANG -> !isTruthy(right);
      MINUS -> {
        checkNumberOperand(expr.operator, right);
        -(right as Double);
      }
      else -> right; // is this right?
    }
  }

  private fun checkNumberOperand(operator: Token, operand: Any?) {
    if (operand is Double) return;
    throw RuntimeError(operator, "Operand must be a number.");
  }

  private fun checkNumberOperands(operator: Token, left: Any?,right: Any?) {
    if (left is Double && right is Double) return;
    
    throw RuntimeError(operator, "Operands must be numbers.");
  }

  private fun isTruthy(obj: Any?): Boolean {
    return when (obj) {
      is Boolean -> obj;
      null -> false;
      else -> true;
    }
  }

  override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
    return evaluate(expr.expression);
  }

  private fun evaluate(expr: Expr): Any? {
    return expr.accept(this);
  }

  private fun execute(stmt: Stmt) {
    stmt.accept(this);
  }

  override fun visitExpressionStmt(stmt: Stmt.Expression): Unit {
    evaluate(stmt.expression);
    return Unit;
  }

  override fun visitPrintStmt(stmt: Stmt.Print): Unit {
    val value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return Unit;
  }

  override fun visitBinaryExpr(expr: Expr.Binary): Any? {
    val left = evaluate(expr.left);
    val right = evaluate(expr.right); 

    return when (expr.operator.type) {
      GREATER -> {
        checkNumberOperands(expr.operator, left, right);
        (left as Double) > (right as Double);
      }
      GREATER_EQUAL -> {
        checkNumberOperands(expr.operator, left, right);
        (left as Double) >= (right as Double);
      }
      LESS -> {
        checkNumberOperands(expr.operator, left, right);
        (left as Double) < (right as Double);
      }
      LESS_EQUAL -> {
        checkNumberOperands(expr.operator, left, right);
        (left as Double) <= (right as Double);
      }
      BANG_EQUAL -> !isEqual(left, right);
      EQUAL_EQUAL -> isEqual(left, right);
      MINUS -> {
        checkNumberOperands(expr.operator, left, right);
        (left as Double) - (right as Double);
      }
      PLUS -> {
        if (left is Double && right is Double) {
          left + right;
        } else if (left is String && right is String) {
          left + right;
        } else {
          throw RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
        }
      }
      SLASH -> {
        checkNumberOperands(expr.operator, left, right);
        (left as Double) / (right as Double);
      }
      STAR -> {
        checkNumberOperands(expr.operator, left, right);
        (left as Double) * (right as Double);
      }
      else -> null; // what about this?
    }
  }

  private fun isEqual(a: Any?, b: Any?): Boolean {
    if (a == null && b == null) return true;
    if (a == null) return false;

    return a.equals(b);
  }

  private fun stringify(obj: Any?): String {
    if (obj == null) return "nil";

    if (obj is Double) {
      var text = obj.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length - 2);
      }

      return text;
    }

    return obj.toString();
  }
}