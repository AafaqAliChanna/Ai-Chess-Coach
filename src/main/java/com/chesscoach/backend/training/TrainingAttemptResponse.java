package com.chesscoach.backend.training;

import java.time.Instant;

public record TrainingAttemptResponse(
        Long id,
        Long gameId,
        int plyNumber,
        String attemptedMoveUci,
        String correctMoveUci,
        boolean wasCorrect,
        Instant attemptedAt
) {}