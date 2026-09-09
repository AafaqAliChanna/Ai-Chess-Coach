package com.chesscoach.backend.analysis;

import java.util.Map;

public record PlayerPatternSummary(
        String playerName,
        int gamesFound,
        int gamesAnalyzed,
        Map<GamePhase, Map<MoveClassification, Integer>> mistakesByPhase
) {}