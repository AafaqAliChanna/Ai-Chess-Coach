package com.chesscoach.backend.analysis;

import java.util.List;

/**
 * Rule-based classification of a single flagged mistake into a PatternTag,
 * using only the game's own already-computed report entries (previous ply's
 * evaluation, this ply's evaluation, and — via SacrificeDetector, the same
 * "actual continuation" technique already proven for Brilliant/Great/Good —
 * the real opponent reply that followed). No hypothetical engine lookahead,
 * no LLM call.
 */
public final class PatternDetector {

    private PatternDetector() {}

    public static PatternTag classify(List<MoveReportEntry> report, int index, boolean moverIsWhite) {
        MoveReportEntry entry = report.get(index);

        // Mover had a forced mate on the board before this move and didn't
        // deliver it. report.get(index-1)'s own mateInMoves field is that
        // earlier position's evaluation with the mover to move — exactly
        // the same value GameReportService itself calls "previousEval."
        if (index > 0) {
            MoveReportEntry previous = report.get(index - 1);
            if (previous.mateInMoves() != null && previous.mateInMoves() > 0) {
                return PatternTag.MISSED_MATE;
            }
        }

        // This move's own stored evaluation (opponent to move) shows a
        // forced mate for the opponent — the mover just walked into it.
        if (entry.mateInMoves() != null && entry.mateInMoves() > 0) {
            return PatternTag.ALLOWED_MATE;
        }

        // Did the opponent's ACTUAL next move (real game continuation, not
        // a hypothetical) capture material for free right after this move?
        if (index + 1 < report.size()) {
            String fenRightAfterThisMove = entry.fenAfter();
            String fenAfterOpponentReply = report.get(index + 1).fenAfter();
            int margin = SacrificeDetector.sacrificeMargin(
                    fenRightAfterThisMove, fenAfterOpponentReply, moverIsWhite);
            if (margin > 0) {
                return PatternTag.HANGING_PIECE;
            }
        }

        return PatternTag.POSITIONAL;
    }
}