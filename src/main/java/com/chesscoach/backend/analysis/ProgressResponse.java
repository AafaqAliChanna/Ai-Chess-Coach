
package com.chesscoach.backend.analysis;

import java.util.List;

public record ProgressResponse(
        String playerName,
        int gamesIncluded,
        int gamesStillAnalyzing,
        List<ProgressEntry> entries
) {}