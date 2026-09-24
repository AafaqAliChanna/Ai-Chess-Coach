package com.chesscoach.backend.analysis;

import java.util.List;

public record MistakeLibraryResponse(
        String playerName,
        int totalMistakes,
        int limit,
        int offset,
        List<MistakeLibraryEntry> entries
) {}