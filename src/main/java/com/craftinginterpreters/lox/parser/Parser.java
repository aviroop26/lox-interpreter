package com.craftinginterpreters.lox.parser;

import com.craftinginterpreters.lox.Lox;
import com.craftinginterpreters.lox.entities.Token;
import com.craftinginterpreters.lox.enums.TokenType;
import com.craftinginterpreters.lox.generated.Expr;
import com.craftinginterpreters.lox.generated.Stmt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.craftinginterpreters.lox.enums.TokenType.*;

public class Parser {

    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * expression → assignment ;
     * @return
     */
    private Expr expression() {
        return assignment();
    }

    /**
     * assignment → IDENTIFIER "=" assignment | equality ;
     * @return
     */
    private Expr assignment() {
        Expr expr = or();

        if(match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if(expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();
        while(match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();
        while(match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    /**
     * equality → comparison ( ( "!=" | "==" ) comparison )* ;
     * @return
     */
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    /**
     * comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
     * @return
     */
    private Expr comparison() {
        Expr expr = term();
        while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * term → factor ( ( "-" | "+" ) factor )* ;
     * @return
     */
    private Expr term() {
        Expr expr = factor();
        while(match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * factor → unary ( ( "/" | "*" ) unary )* ;
     * @return
     */
    private Expr factor() {
        Expr expr = unary();
        while(match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * unary → ( "!" | "-" ) unary | primary ;
     * @return
     */
    private Expr unary() {
        if(match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    /**
     * call → primary ( "(" arguments? ")" )* ;
     * @return
     */
    private Expr call() {
        Expr expr = primary();
        while(true) {
            if(match(LEFT_PAREN)) {
                expr = finishCall(expr);
            }
            else {
                break;
            }
        }
        return expr;
    }

    /**
     * arguments → expression ( "," expression )* ;
     * @param callee
     * @return
     */
    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if(!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while(match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expect ) after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }

    /**
     * primary → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
     * @return
     */
    private Expr primary() {
        if(match(FALSE)) return new Expr.Literal(false);
        if(match(TRUE)) return new Expr.Literal(true);
        if(match(NIL)) return new Expr.Literal(null);

        if(match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if(match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if(match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }

    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    /**
     * declaration → varDecl | statement ;
     * @return
     */
    private Stmt declaration() {
        try {
            if(match(FUN)) return function("function");
            if(match(VAR)) return varDeclaration();
            return statement();
        } catch(ParseError e) {
            synchronize();
            return null;
        }
    }

    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect ( after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if(!check(RIGHT_PAREN)) {
            do {
                if(parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while(match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ) after parameters.");
        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    /**
     * varDecl → "var" IDENTIFIER ( "=" expression )? ";" ;
     * @return
     */
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        Expr initializer = null;
        if(match(EQUAL)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ; after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    /**
     * statement → exprStmt | printStmt | block | ifStmt | whileStmt | forStmt;
     * @return
     */
    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        if (match(RETURN)) return returnStatement();

        return expressionStatement();
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    /**
     * forStmt → "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement ;
     * @return
     */
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expected '(' after 'for'.");

        Stmt initializer = null;
        if(match(SEMICOLON)) {
            initializer = null;
        }
        else if(match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if(!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expected ';' after loop condition.");

        Expr increment = null;
        if(!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expected ')' after for clause.");
        Stmt body = statement();

        if(increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }
        if(condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);
        if(initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    /**
     * whileStmt → "while" "(" expression ")" statement ;
     * @return
     */
    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expected '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ')' after while condition.");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    /**
     * ifStmt → "if" "(" expression ")" statement ( "else" statement )? ;
     * @return
     */
    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if(match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    /**
     * block → "{" declaration* "}" ;
     * @return
     */
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while(!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    /**
     * printStmt → "print" expression ";" ;
     * @return
     */
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    /**
     * exprStmt → expression ";" ;
     * @return
     */
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }
}