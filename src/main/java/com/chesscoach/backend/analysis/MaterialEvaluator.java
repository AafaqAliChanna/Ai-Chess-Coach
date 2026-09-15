package com.chesscoach.backend.analysis;

/**
 * Computes material balance from a FEN's piece-placement field, using
 * standard piece values (pawn=1, knight=3, bishop=3, rook=5, queen=9;
 * kings excluded — they're never captured/traded).
 *
 * Distinct from GamePhase's material weighting: GamePhase weighs pieces by
 * how much they contribute to POSITION COMPLEXITY (knight/bishop=1, rook=2,
 * queen=4 — its own separate scale, used only to detect "has the position
 * simplified"). This class computes actual material VALUE for detecting
 * genuine sacrifices — a different purpose, deliberately not merged into
 * one shared number that would conflate two unrelated concerns.
 */
public class MaterialEvaluator {

    private MaterialEvaluator() {}

    public static int pieceValue(char pieceChar) {
        return switch (Character.toLowerCase(pieceChar)) {
            case 'p' -> 1;
            case 'n', 'b' -> 3;
            case 'r' -> 5;
            case 'q' -> 9;
            default -> 0; // king, digits, slashes
        };
    }

    /** White's total material value minus Black's, from a FEN's piece-placement field. */
    public static int materialBalance(String fen) {
        String piecePlacement = fen.split(" ")[0];
        int balance = 0;
        for (char c : piecePlacement.toCharArray()) {
            int value = pieceValue(c);
            if (value == 0) continue;
            balance += Character.isUpperCase(c) ? value : -value;
        }
        return balance;
    }
}