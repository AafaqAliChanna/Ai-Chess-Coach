package com.chesscoach.backend.common;

import com.chesscoach.backend.game.InvalidPgnException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorResponse(Instant timestamp, int status, String error, String message) {}

    @ExceptionHandler(InvalidPgnException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPgn(InvalidPgnException ex) {
        log.info("Rejected PGN upload: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(
                Instant.now(), 400, "Invalid PGN", ex.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                Instant.now(), 500, "Internal Server Error",
                "Something went wrong on our end."
        ));
    }
}