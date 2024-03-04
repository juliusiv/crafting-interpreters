package com.craftinginterpreters.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

@Throws(IOException::class)
fun main(args: Array<String>) {
  if (args.size != 1) {
    System.err.println("Usage: generate_ast <output directory>");
    System.exit(64);
  }
  val outputDir = args[0];

  defineAst(outputDir, "Expr", Arrays.asList(
    "Assign   : name: Token, value: Expr",
    "Binary   : left: Expr, operator: Token, right: Expr",
    "Grouping : expression: Expr",
    "Literal  : value: Any?",
    "Unary    : operator: Token, right: Expr",
    "Variable : name: Token"
  ));

  defineAst(outputDir, "Stmt", Arrays.asList(
    "Block      : statements: List<Stmt>",
    "Expression : expression: Expr",
    "Print      : expression: Expr",
    "Var        : name: Token, initializer: Expr?"
  ));
}

@Throws(IOException::class)
private fun defineAst(outputDir: String, baseName: String, types: List<String>) {
  val path = outputDir + "/" + baseName + ".kt";
  val writer = PrintWriter(path, "UTF-8");

  writer.println("package com.craftinginterpreters.lox;");
  writer.println();
  writer.println("import kotlin.collections.List;");
  writer.println();

  
  // Base interface
  writer.println("interface " + baseName + " {");

  defineVisitor(writer, baseName, types);
  writer.println();

  writer.println("  fun <R> accept(visitor: Visitor<R>): R;");
  writer.println();

  for (type in types) {
    val className = type.split(":")[0].trim();
    val fields = type.split(":", limit = 2)[1].trim(); 
    defineType(writer, baseName, className, fields);
    writer.println();
  }

  writer.println("}");
  writer.println();

  writer.close();
}

fun defineVisitor(writer: PrintWriter, baseName: String, types: List<String>) {
  writer.println("  interface Visitor<out R> {");

  for (type in types) {
    val typeName = type.split(":")[0].trim();
    writer.println("    fun visit" + typeName + baseName + "(" + baseName.lowercase() + ": " + baseName + "." + typeName + "): R;");
  }

  writer.println("  }");
}

fun defineType(writer: PrintWriter, baseName: String, className: String, fieldList: String) {
  val vals = fieldList.split(",").joinToString(", val ", prefix = "val ");
  writer.println("  class " + className + "(" + vals + ")" + " : " + baseName + " {");

  // Visitor pattern.
  writer.println("    override fun <R> accept(visitor: Visitor<R>): R {");
  writer.println("      return visitor.visit" + className + baseName + "(this);");
  writer.println("    }");

  writer.println("  }");
}