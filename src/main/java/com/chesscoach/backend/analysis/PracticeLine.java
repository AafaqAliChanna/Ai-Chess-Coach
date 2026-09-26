package com.chesscoach.backend.analysis;

import java.util.List;

public record PracticeLine(
        Integer evalCp,
        Integer evalMate,
        List<String> movesUci,
        List<String> movesSan,
        String fenAfterFirstMove
) {}