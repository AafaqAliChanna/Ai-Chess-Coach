package com.chesscoach.backend.analysis.engine;

public record CandidateMove(int rank, String moveUci, Integer scoreCentipawns, Integer mateInMoves) {}