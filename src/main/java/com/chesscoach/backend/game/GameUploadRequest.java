package com.chesscoach.backend.game;

import jakarta.validation.constraints.NotBlank;

public record GameUploadRequest(
        @NotBlank(message = "PGN text is required")
        String pgn,
        String whitePlayer,
        String blackPlayer,
        String result
) {}