package com.craftinginterpreters.lox;

import java.util.HashMap;
import kotlin.collections.List;
import kotlin.collections.Map;
import java.util.Stack;

class Resolver(val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
  private final val scopes = Stack<MutableMap<String, Boolean>>();
  private var currentFunction: FunctionType = FunctionType.NONE;

  fun resolve(statements: List<Stmt>) {
    for (statement in statements) {
      resolve(statement);
    }
  }

  public override fun visitBlockStmt(stmt: Stmt.Block): Unit {
    beginScope();
    resolve(stmt.statements);
    endScope();
  }

  public override fun visitExpressionStmt(stmt: Stmt.Expression): Unit {
    resolve(stmt.expression);
    return Unit;
  }

  public override fun visitVarStmt(stmt: Stmt.Var): Unit {
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);

    return Unit;
  }

  public override fun visitWhileStmt(stmt: Stmt.While): Unit {
    resolve(stmt.condition);
    resolve(stmt.body);
    return Unit;
  }

  public override fun visitVariableExpr(expr: Expr.Variable): Unit {
    if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == false) {
    // if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
      loxError(expr.name,
          "Can't read local variable in its own initializer.");
    }

    resolveLocal(expr, expr.name);
    return Unit;
  }

  public override fun visitAssignExpr(expr: Expr.Assign): Unit {
    resolve(expr.value);
    resolveLocal(expr, expr.name);

    return Unit;
  }

  public override fun visitBinaryExpr(expr: Expr.Binary): Unit {
    resolve(expr.left);
    resolve(expr.right);
    return Unit;
  }

  public override fun visitCallExpr(expr: Expr.Call): Unit {
    resolve(expr.callee);

    for (argument in expr.arguments) {
      resolve(argument);
    }

    return Unit;
  }

  public override fun visitGroupingExpr(expr: Expr.Grouping): Unit {
    resolve(expr.expression);
    return Unit;
  }

  public override fun visitLiteralExpr(expr: Expr.Literal): Unit {
    return Unit;
  }

  public override fun visitLogicalExpr(expr: Expr.Logical): Unit {
    resolve(expr.left);
    resolve(expr.right);
    return Unit;
  }

  public override fun visitUnaryExpr(expr: Expr.Unary): Unit {
    resolve(expr.right);
    return Unit;
  }

  public override fun visitFunctionStmt(stmt: Stmt.Function): Unit {
    declare(stmt.name);
    define(stmt.name);

    resolveFunction(stmt, FunctionType.FUNCTION);
    return Unit;
  }

  public override fun visitIfStmt(stmt: Stmt.If): Unit {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null) resolve(stmt.elseBranch);
    return Unit;
  }

  public override fun visitPrintStmt(stmt: Stmt.Print): Unit {
    resolve(stmt.expression);
    return Unit;
  }

  public override fun visitReturnStmt(stmt: Stmt.Return): Unit {
    if (currentFunction == FunctionType.NONE) {
      loxError(stmt.keyword, "Can't return from top-level code.");
    }

    if (stmt.value != null) {
      resolve(stmt.value);
    }

    return Unit;
  }

  private fun resolve(stmt: Stmt) {
    stmt.accept(this);
  }

  private fun resolve(expr: Expr) {
    expr.accept(this);
  }

  private fun beginScope() {
    scopes.push(HashMap<String, Boolean>());
  }

  private fun endScope() {
    scopes.pop();
  }

  private fun declare(name: Token) {
    if (scopes.isEmpty()) return;

    val scope = scopes.peek();
    if (scope.containsKey(name.lexeme)) {
      loxError(name, "Already a variable with this name in this scope.");
    }

    scope.put(name.lexeme, false);
  }

  private fun define(name: Token) {
    if (scopes.isEmpty()) return;

    scopes.peek().put(name.lexeme, true);
  }

  private fun resolveLocal(expr: Expr, name: Token) {
    for (i in scopes.indices.reversed()) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size - 1 - i);
        return;
      }
    }
  }

  private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
    val enclosingFunction = currentFunction;
    currentFunction = type;

    beginScope();
    for (param in function.params) {
      declare(param);
      define(param);
    }
    resolve(function.body);
    endScope();

    currentFunction = enclosingFunction;
  }
}

private enum class FunctionType {
  NONE,
  FUNCTION
}