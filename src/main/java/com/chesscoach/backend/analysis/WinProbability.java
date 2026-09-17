package com.chesscoach.backend.analysis;

/**
 * Converts a Stockfish evaluation (centipawns or forced mate) into a win
 * probability (0-100) for the side to move, using the logistic formula
 * Lichess publishes and uses in production:
 * Win% = 50 + 50 * (2 / (1 + exp(-0.00368208 * centipawns)) - 1)
 * (see lichess.org/page/accuracy)
 *
 * Why this matters: raw centipawn differences are NOT linear in practical
 * significance. Going from +900 to +400 barely changes winning chances
 * (still completely winning); going from +50 to -50 is a huge swing
 * (equal to losing). Measuring "how bad was this move" in win-probability
 * terms instead of raw centipawns fixes exactly this distortion.
 */
public final class WinProbability {

    private WinProbability() {}

    private static final double LOGISTIC_CONSTANT = 0.00368208;

    /**
     * @param centipawns  engine score, from the perspective of the side to
     *                    move (null if mateInMoves is set instead)
     * @param mateInMoves forced mate distance; positive = side to move
     *                    delivers mate, negative = side to move gets mated
     *                    (null if centipawns is set instead)
     * @return win probability (0-100) for the side to move
     */
    public static double fromEvaluation(Integer centipawns, Integer mateInMoves) {
        if (mateInMoves != null) {
            // A forced mate is a certain, theoretical win or loss \u2014 capped
            // at the extremes rather than run through the centipawn formula.
            return mateInMoves > 0 ? 100.0 : 0.0;
        }
        int cp = (centipawns != null) ? centipawns : 0;
        return 50 + 50 * (2.0 / (1 + Math.exp(-LOGISTIC_CONSTANT * cp)) - 1);
    }
}