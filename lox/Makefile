run-prompt:
	kotlinc *.kt -include-runtime -d main.jar && java -jar main.jar

run:
	kotlinc *.kt -include-runtime -d main.jar && java -jar main.jar $(file)

generate-ast:
	kotlinc ../tools/GenerateAst.kt -include-runtime -d generate_ast.jar && java -jar generate_ast.jar .