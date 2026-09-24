package com.chesscoach.backend.analysis;

import java.time.Instant;

public record MistakeLibraryEntry(
        Long gameId,
        String gameTitle,
        Instant uploadedAt,
        int plyNumber,
        String fenBefore,
        String playerMove,
        String bestMoveUci,
        MoveClassification classification,
        GamePhase gamePhase,
        long centipawnLoss,
        double winPercentLoss
) {}