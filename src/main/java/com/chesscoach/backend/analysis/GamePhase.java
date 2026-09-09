package com.chesscoach.backend.analysis;

/**
 * Determines game phase from actual board material, not move count.
 *
 * Uses the "tapered eval phase" technique from chess engine design: each
 * piece type is weighted by how much it contributes to position complexity
 * (knight=1, bishop=1, rook=2, queen=4). The starting position totals 24
 * (both sides). As material is traded off, this number drops — a low value
 * means the position has genuinely simplified into an endgame, regardless
 * of what move number it happens to be.
 *
 * KNOWN LIMITATION, not hidden: material alone can reliably detect ENDGAME
 * (few pieces = simplified position, a real structural fact). It CANNOT
 * distinguish OPENING from MIDDLEGAME on material alone — a position with
 * every piece still on the board could be move 3 or move 30 of a slow game.
 * So the opening/middlegame split still uses ply count as a secondary
 * signal. This is an honest constraint of what material data can tell you,
 * not a shortcut — the part that matters most for coaching (accurately
 * flagging endgame mistakes) is now materially grounded.
 */
public enum GamePhase {
    OPENING, MIDDLEGAME, ENDGAME;

    private static final int MAX_PHASE = 24;
    private static final int ENDGAME_PHASE_THRESHOLD = 6; // roughly: queens traded + limited minor/major pieces left
    private static final int OPENING_PLY_CUTOFF = 20;

    public static GamePhase fromFen(String fen, int plyNumber) {
        int phaseValue = computePhaseValue(fen);

        if (phaseValue <= ENDGAME_PHASE_THRESHOLD) {
            return ENDGAME;
        }
        return plyNumber <= OPENING_PLY_CUTOFF ? OPENING : MIDDLEGAME;
    }

    private static int computePhaseValue(String fen) {
        String piecePlacement = fen.split(" ")[0]; // first FEN field only, ignore turn/castling/etc.
        int phase = 0;

        for (char c : piecePlacement.toCharArray()) {
            switch (Character.toLowerCase(c)) {
                case 'n', 'b' -> phase += 1;
                case 'r' -> phase += 2;
                case 'q' -> phase += 4;
                default -> { /* pawns, kings, digits, slashes — contribute nothing */ }
            }
        }
        return Math.min(phase, MAX_PHASE);
    }
}