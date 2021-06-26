path ?= ./script.lox

compile:
	mvn compile
run:
	mvn compile exec:java -Dexec.mainClass="com.craftinginterpreters.lox.Lox"
run-script:
	mvn compile exec:java -Dexec.mainClass="com.craftinginterpreters.lox.Lox" -Dexec.arguments="$(path)"