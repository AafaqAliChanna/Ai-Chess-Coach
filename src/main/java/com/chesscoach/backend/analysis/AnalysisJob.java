package com.chesscoach.backend.analysis;

import java.time.Instant;

/**
 * Represents "analyze this game with Stockfish" as a unit of queued work.
 * Deliberately minimal — the worker re-fetches the Game/Move rows itself
 * rather than us stuffing move data into the job. Keeping the job tiny
 * means it serializes cheaply into Redis and stays valid even if the
 * game's data changes between enqueue and processing.
 */
public record AnalysisJob(Long gameId, Instant requestedAt) {

    public static AnalysisJob forGame(Long gameId) {
        return new AnalysisJob(gameId, Instant.now());
    }
}