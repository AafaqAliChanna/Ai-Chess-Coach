package com.chesscoach.backend.integrations.chesscom;

public record ChessComImportResult(int gamesFoundForTimeClass, int imported, int skippedAsDuplicate) {}