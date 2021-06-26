package com.craftinginterpreters.lox.scanner;

import com.craftinginterpreters.lox.entities.Token;

import java.util.List;

public interface Scanner {
    List<Token> scanTokens();
}
