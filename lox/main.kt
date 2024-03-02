package com.craftinginterpreters.lox

import java.io.IOException
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.charset.Charset
import java.io.InputStreamReader
import java.io.BufferedReader

var hadError = false;
var hadRuntimeError = false;

fun main(args: Array<String>) {
  // printAst()

  if (args.size > 1) {
    System.out.println("Usage: jlox [script]");
    System.exit(64); 
  } else if (args.size == 1) {
    runFile(args[0]);
  } else {
    runPrompt();
  }
}

fun printAst() {
  val expression = Expr.Binary(
    Expr.Unary(
      Token(TokenType.MINUS, "-", null, 1),
      Expr.Literal(123)
    ),
    Token(TokenType.STAR, "*", null, 1),
    Expr.Grouping(
      Expr.Literal(45.67)
    )
  );

  System.out.println(AstPrinter().print(expression));
}

@Throws(IOException::class)
fun runFile(path: String) {
  val bytes = Files.readAllBytes(Paths.get(path));
  run(String(bytes, Charset.defaultCharset()));

  if (hadError) System.exit(65);
  if (hadRuntimeError) System.exit(70);
}

@Throws(IOException::class)
fun runPrompt() {
  val input = InputStreamReader(System.`in`);
  val reader = BufferedReader(input);

  while (true) {
    System.out.print("> ");
    val line = reader.readLine();
    if (line == null) break;

    run(line);

    hadError = false;
  }
}

fun run(source: String) {
  val scanner = com.craftinginterpreters.lox.Scanner(source);
  val tokens = scanner.scanTokens();

  val parser = Parser(tokens);
  val statements = parser.parse();

  // Stop if there was a syntax error.
  if (hadError) {
    System.out.println("Syntax error!");
    return;
  }

  val interpreter = Interpreter();
  interpreter.interpret(statements);

  // System.out.println(AstPrinter().print(expression) + "\n");
  // for (token in tokens) {
  //   System.out.println(token.toString());
  // }
}

fun error(line: Int, message: String) {
  report(line, "", message);
}

private fun report(line: Int, where: String, message: String) {
  System.err.println("[line " + line + "] Error" + where + ": " + message);
  hadError = true;
}

fun loxError(token: Token, message: String) {
  if (token.type == TokenType.EOF) {
    report(token.line, " at end", message);
  } else {
    report(token.line, " at '" + token.lexeme + "'", message);
  }
}

fun loxRuntimeError(error: RuntimeError) {
  System.err.println(error.message +
      "\n[line " + error.token.line + "]");
  hadRuntimeError = true;
}