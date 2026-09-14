package com.chesscoach.backend.analysis;

import java.time.Instant;
import java.util.Map;

public record ProgressEntry(
        Long gameId,
        String title,
        Instant uploadedAt,
        int totalMoves,
        Map<MoveClassification, Integer> mistakeCounts
) {}