package com.chesscoach.backend.analysis.engine;

/**
 * The result of asking Stockfish to evaluate one position.
 * Exactly one of scoreCentipawns / mateInMoves is non-null:
 * - scoreCentipawns: normal evaluation, from the perspective of the side to move.
 *   Positive = that side is better. 100 centipawns ≈ "worth one pawn."
 * - mateInMoves: forced mate found. Positive = side to move delivers mate;
 *   negative = side to move gets mated. The number is moves-to-mate, not plies.
 */
public record EngineEvaluation(String bestMoveUci, Integer scoreCentipawns, Integer mateInMoves) {

    public boolean isMate() {
        return mateInMoves != null;
    }
}