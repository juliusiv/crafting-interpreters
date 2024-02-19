package com.craftinginterpreters.lox

import java.io.IOException
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.charset.Charset
import java.io.InputStreamReader
import java.io.BufferedReader
// import com.craftinginterpreters.lox.Scanner

var hadError = false

fun main(args: Array<String>) {
    if (args.size > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64); 
    } else if (args.size == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
}

@Throws(IOException::class)
fun runFile(path: String) {
  val bytes = Files.readAllBytes(Paths.get(path));
  run(String(bytes, Charset.defaultCharset()));

  if (hadError) System.exit(65)
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

  // For now, just print the tokens.
  for (token in tokens) {
    System.out.println(token.toString());
  }
}

fun error(line: Int, message: String) {
  report(line, "", message);
}

private fun report(line: Int, where: String, message: String) {
  System.err.println("[line " + line + "] Error" + where + ": " + message);
  hadError = true;
}