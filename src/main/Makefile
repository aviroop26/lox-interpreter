path ?= ./resources/script.lox

compile:
	javac java/com/craftinginterpreters/lox/*.java -d classes
run:
	javac java/com/craftinginterpreters/lox/*.java -d classes
	java -cp classes com.craftinginterpreters.lox.Lox
run-script:
	javac java/com/craftinginterpreters/lox/*.java -d classes
	java -cp classes com.craftinginterpreters.lox.Lox $(path)