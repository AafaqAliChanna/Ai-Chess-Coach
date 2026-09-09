package com.chesscoach.backend.analysis;

public record MoveReportEntry(
        int plyNumber,
        String san,
        String fenAfter,
        String bestMoveUci,
        Integer scoreCentipawns,
        Integer mateInMoves,
        long centipawnLoss,
        MoveClassification classification,
        GamePhase gamePhase
) {}