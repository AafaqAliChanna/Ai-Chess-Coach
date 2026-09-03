package com.chesscoach.backend.game;

public class InvalidPgnException extends RuntimeException {
    public InvalidPgnException(String message) {
        super(message);
    }

    public InvalidPgnException(String message, Throwable cause) {
        super(message, cause);
    }
}