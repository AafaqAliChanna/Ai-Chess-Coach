package com.chesscoach.backend.analysis.engine;

public class EngineException extends RuntimeException {
    public EngineException(String message) {
        super(message);
    }

    public EngineException(String message, Throwable cause) {
        super(message, cause);
    }
}