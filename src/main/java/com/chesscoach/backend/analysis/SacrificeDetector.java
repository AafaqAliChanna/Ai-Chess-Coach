package com.chesscoach.backend.analysis;

/**
 * Detects whether a move was a real material sacrifice, using the actual
 * game continuation (the opponent's real next move) rather than a
 * hypothetical engine lookahead — this data is already stored from PGN
 * parsing, so no extra Stockfish calls are needed for this check.
 */
public class SacrificeDetector {

    private SacrificeDetector() {}

    /**
     * @param fenBeforeMove  position immediately before the mover's move
     * @param fenAfterReply  position after the OPPONENT'S actual next move
     *                       in the real game (null if this was the game's
     *                       final move — no reply exists, so no sacrifice
     *                       can be confirmed)
     * @param moverIsWhite   which side made the move being evaluated
     * @return material margin given up (positive = a real sacrifice of that
     *         many points' worth; zero or negative = not a sacrifice)
     */
    public static int sacrificeMargin(String fenBeforeMove, String fenAfterReply, boolean moverIsWhite) {
        if (fenAfterReply == null) {
            return 0;
        }

        int balanceBefore = MaterialEvaluator.materialBalance(fenBeforeMove);
        int balanceAfterReply = MaterialEvaluator.materialBalance(fenAfterReply);

        // materialBalance() is White-minus-Black — flip sign so both values
        // are expressed from the MOVER's own perspective before comparing.
        int moverBalanceBefore = moverIsWhite ? balanceBefore : -balanceBefore;
        int moverBalanceAfterReply = moverIsWhite ? balanceAfterReply : -balanceAfterReply;

        return moverBalanceBefore - moverBalanceAfterReply;
    }
}