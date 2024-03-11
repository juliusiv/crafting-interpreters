package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.TokenType.*;
import com.craftinginterpreters.lox.RuntimeError

class ClockNative : LoxCallable {
  override fun arity(): Int { return 0; }

  override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
    return System.currentTimeMillis() / 1000.0;
  }

  override fun toString(): String { return "<native fn>"; }
};

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
  final var globals = Environment();
  private final val locals: MutableMap<Expr, Int> = mutableMapOf();
  private var environment = globals;

  constructor() {
    globals.define("clock", ClockNative() );
  }

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

  override fun visitLogicalExpr(expr: Expr.Logical): Any? {
    val left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }

    return evaluate(expr.right);
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

  override fun visitVariableExpr(expr: Expr.Variable): Any? {
    return lookUpVariable(expr.name, expr);
  }

  private fun lookUpVariable(name: Token, expr: Expr): Any? {
    val distance = locals.get(expr);
    if (distance != null) {
      return environment.getAt(distance, name.lexeme);
    } else {
      return globals.get(name);
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

  fun resolve(expr: Expr, depth: Int) {
    locals.put(expr, depth);
  }

  fun executeBlock(statements: List<Stmt>, environment: Environment) {
    val previous = this.environment;
    try {
      this.environment = environment;

      for (statement in statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  override fun visitBlockStmt(stmt: Stmt.Block): Unit {
    executeBlock(stmt.statements, Environment(environment));
    return Unit;
  }

  override fun visitExpressionStmt(stmt: Stmt.Expression): Unit {
    evaluate(stmt.expression);
    return Unit;
  }

  override fun visitFunctionStmt(stmt: Stmt.Function): Unit {
    val function = LoxFunction(stmt, environment);
    environment.define(stmt.name.lexeme, function);

    return Unit;
  }

  override fun visitIfStmt(stmt: Stmt.If): Unit {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }

    return Unit;
  }

  override fun visitPrintStmt(stmt: Stmt.Print): Unit {
    val value = evaluate(stmt.expression);
    System.out.println(stringify(value));

    return Unit;
  }

  override fun visitReturnStmt(stmt: Stmt.Return): Unit {
    var value: Any? = null;
    if (stmt.value != null) value = evaluate(stmt.value);

    throw Return(value);
  }

  override fun visitVarStmt(stmt: Stmt.Var): Unit {
    var value: Any? = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    environment.define(stmt.name.lexeme, value);
    // return null;
  }

  override fun visitWhileStmt(stmt: Stmt.While): Unit {
    while(isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body);
    }

    return Unit;
  }

  override fun visitAssignExpr(expr: Expr.Assign): Any? {
    val value = evaluate(expr.value);

    val distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, expr.name, value);
    } else {
      globals.assign(expr.name, value);
    }

    return value;
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

  
  override fun visitCallExpr(expr: Expr.Call): Any? {
    val callee = evaluate(expr.callee);

    val arguments = ArrayList<Any?>();
    for (argument in expr.arguments) { 
      arguments.add(evaluate(argument));
    }

    if (!(callee is LoxCallable)) {
      throw RuntimeError(expr.paren, "Can only call functions and classes.");
    }

    val function: LoxCallable = callee;

    if (arguments.size != function.arity()) {
      throw RuntimeError(expr.paren, "Expected ${function.arity()} arguments but got ${arguments.size}.");
    }

    return function.call(this, arguments);
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