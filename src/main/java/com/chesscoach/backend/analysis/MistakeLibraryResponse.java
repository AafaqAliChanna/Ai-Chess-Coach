package com.chesscoach.backend.analysis;

import java.util.List;
import java.util.Map;

public record MistakeLibraryResponse(
        String playerName,
        int totalMistakes,
        int limit,
        int offset,
        Map<PatternTag, Integer> patternCounts,
        List<MistakeLibraryEntry> entries
) {}