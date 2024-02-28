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
    "Binary   : Expr left, Token operator, Expr right",
    "Grouping : Expr expression",
    "Literal  : Object value",
    "Unary    : Token operator, Expr right"
  ));
}

@Throws(IOException::class)
private fun defineAst(outputDir: String, baseName: String, types: List<String>) {
  val path = outputDir + "/" + baseName + ".kt";
  val writer = PrintWriter(path, "UTF-8");

  writer.println("package com.craftinginterpreters.lox;");
  writer.println();
  writer.println("import java.util.List;");
  writer.println();
  writer.println("abstract class " + baseName + " {");

  defineVisitor(writer, baseName, types);

  for (type in types) {
    val className = type.split(":")[0].trim();
    val fields = type.split(":")[1].trim(); 
    defineType(writer, baseName, className, fields);
  }

  // The base accept() method.
  writer.println();
  writer.println("  abstract <R> R accept(Visitor<R> visitor);");

  writer.println("}");
  writer.close();
}

fun defineVisitor(writer: PrintWriter, baseName: String, types: List<String>) {
  writer.println("  interface Visitor<R> {");

  for (type in types) {
    val typeName = type.split(":")[0].trim();
    writer.println("    R visit" + typeName + baseName + "(" + typeName + " " + baseName.lowercase() + ");");
  }

  writer.println("  }");
}

fun defineType(writer: PrintWriter, baseName: String, className: String, fieldList: String) {
  writer.println("  static class " + className + " extends " + baseName + " {");

  // Constructor.
  writer.println("    " + className + "(" + fieldList + ") {");

  // Store parameters in fields.
  val fields = fieldList.split(", ");
  for (field in fields) {
    val name = field.split(" ")[1];
    writer.println("      this." + name + " = " + name + ";");
  }

  writer.println("    }");

  // Visitor pattern.
  writer.println();
  writer.println("    @Override");
  writer.println("    <R> R accept(Visitor<R> visitor) {");
  writer.println("      return visitor.visit" + className + baseName + "(this);");
  writer.println("    }");

  // Fields.
  writer.println();
  for (field in fields) {
    writer.println("    final " + field + ";");
  }

  writer.println("  }");
}