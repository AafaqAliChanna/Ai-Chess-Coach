package com.chesscoach.backend.coach;

public class CoachingException extends RuntimeException {
    public CoachingException(String message) { super(message); }
    public CoachingException(String message, Throwable cause) { super(message, cause); }
}