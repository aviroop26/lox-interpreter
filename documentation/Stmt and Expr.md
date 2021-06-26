## Stmt and Expr:

Stmt and Expr are two abstract classes that are generated from the GenerateAst class.

Stmt consists of:
1. If statement
2. Block statement
3. Expression statement
4. Print statement
5. Var statement
6. While statement

Expr consists of:
1. Assign expression
2. Binary expression
3. Grouping expression
4. Literal expression
5. Logical expression
6. Unary expression
7. Variable expression

Stmt always return a void type whereas Expr always returns an object.

## Visitor Pattern

Both of them have a visitor interface which has function declaration for each type of implementation.

For example, the Expr.Visitor interface has:
1. visitAssignExpr
2. visitBinaryExpr
3. visitUnaryExpr and so on ...

The Interpreter implements the visitor interface of both Expr and Stmt.
The output of the parser is a list of Stmt.
The interpreter's job is to execute these statements sequentially.

Both the Expr and Stmt have an accept method which all the child classes inherit.
The visitor just calls the accept method of both Expr and Stmt passing itself.
Now, based of which class is implementing the Expr or Stmt, the visitor function of the Interpreter is called.

For example,

Interpreter object: I
Stmt.Binary object: SB

1. I.execute(SB)
2. I calls SB.accept(I)
3. SB.accept(I) calls I.visitBinaryStmt(SB)
4. visitBinaryStmt has the core logic regarding the implementation of Stmt.Binary.