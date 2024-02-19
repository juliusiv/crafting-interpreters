# Crafting Interpreters
My code for [_Crafting Interpreters_](https://craftinginterpreters.com/). I'm using Kotlin where the book uses Java. We'll see about the C parts. Maybe some Rust?

```bash
# Get the kotlin compiler
brew update
brew install kotlin

# Compile the thing
kotlinc *.kt -include-runtime -d main.jar

# Run the thing
java -jar main.jar
```