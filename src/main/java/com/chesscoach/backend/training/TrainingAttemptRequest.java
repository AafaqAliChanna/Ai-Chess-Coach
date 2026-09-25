package com.chesscoach.backend.training;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TrainingAttemptRequest(
        @NotNull(message = "gameId is required") Long gameId,
        int plyNumber,
        @NotBlank(message = "attemptedMoveUci is required") String attemptedMoveUci
) {}