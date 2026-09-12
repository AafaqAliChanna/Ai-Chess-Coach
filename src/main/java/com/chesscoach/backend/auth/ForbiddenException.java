package com.chesscoach.backend.auth;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) { super(message); }
}