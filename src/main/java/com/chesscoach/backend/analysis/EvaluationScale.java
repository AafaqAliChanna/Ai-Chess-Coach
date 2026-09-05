package com.chesscoach.backend.analysis;

/**
 * Converts Stockfish's two distinct evaluation types (centipawns vs. forced
 * mate) into one comparable long, so move-to-move swings can be measured
 * with plain subtraction regardless of whether either side is a mate score.
 *
 * Mate scores are mapped far outside any realistic centipawn range so they
 * always dominate a comparison against a plain centipawn score — losing a
 * forced mate should always register as catastrophic, never get diluted
 * by being averaged against small centipawn numbers.
 */
public final class EvaluationScale {

    private static final long MATE_BASE = 1_000_000L;

    private EvaluationScale() {}

    public static long toComparable(Integer scoreCentipawns, Integer mateInMoves) {
        if (mateInMoves != null) {
            return mateInMoves > 0
                    ? MATE_BASE - mateInMoves   // mate FOR this side: sooner mate = higher score
                    : -MATE_BASE - mateInMoves; // mate AGAINST this side: sooner mate = more negative
        }
        return scoreCentipawns != null ? scoreCentipawns : 0;
    }

    /** Flips a comparable score to the opposite side's perspective. */
    public static long negate(long comparableScore) {
        return -comparableScore;
    }
}