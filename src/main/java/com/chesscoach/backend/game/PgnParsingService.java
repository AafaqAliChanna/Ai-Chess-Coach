package com.chesscoach.backend.game;

import com.github.bhlangonijr.chesslib.Board;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PgnParsingService {

    /**
     * Parses SAN move text from a PGN's movetext section and replays it on a board,
     * producing one domain Move per ply with the resulting FEN.
     *
     * Deliberately does NOT parse PGN tags (event, date, etc.) into structured
     * data — Game already carries whitePlayer/blackPlayer/result from the
     * upload request or the import service. This method's only job is turning
     * move text into a validated, position-annotated move list; header tags
     * are stripped and discarded, not read.
     */
    public List<Move> parseMoves(Game game, String pgnMoveText) {
        Board board = new Board(); // starts at the standard initial position

        List<String> sanTokens = extractSanTokens(pgnMoveText);
        List<Move> moves = new ArrayList<>();

        int ply = 1;
        for (String san : sanTokens) {
            try {
                board.doMove(san);
            } catch (Exception e) {
                throw new InvalidPgnException(
                        "Illegal or unparseable move \"" + san + "\" at ply " + ply
                                + ". Game rejected — a corrupt move list would poison every "
                                + "downstream analysis.", e);
            }
            moves.add(new Move(game, ply, san, board.getFen()));
            ply++;
        }

        if (moves.isEmpty()) {
            throw new InvalidPgnException("No moves found in PGN — nothing to store.");
        }

        return moves;
    }

    /**
     * Strips PGN header tags, comments, variations, move numbers, and the
     * trailing result marker from raw PGN text, leaving just the SAN tokens
     * in order.
     *
     * Header tags (e.g. [Event "Live Chess"], [White "someone"]) appear as
     * their own lines at the top of any real-world PGN export — Chess.com's
     * export includes them, hand-typed test PGNs typically don't, which is
     * why this gap went unnoticed until real Chess.com data hit it. Stripped
     * here so this method works correctly on BOTH bare movetext and a full,
     * header-included PGN file.
     */
    private List<String> extractSanTokens(String pgnMoveText) {
        String cleaned = pgnMoveText
                .replaceAll("(?m)^\\[.*]\\s*$", "")           // strip PGN header tags, e.g. [Event "Live Chess"]
                .replaceAll("\\{[^}]*}", "")                   // strip {comments}
                .replaceAll("\\([^)]*\\)", "")                 // strip (variations) — not supported yet, dropped not crashed
                .replaceAll("\\d+\\.(\\.\\.)?", "")            // strip move numbers like "1." and "1..."
                .replaceAll("(1-0|0-1|1/2-1/2|\\*)\\s*$", "")  // strip trailing result
                .trim();

        List<String> tokens = new ArrayList<>();
        for (String token : cleaned.split("\\s+")) {
            if (!token.isBlank()) {
                tokens.add(token.trim());
            }
        }
        return tokens;
    }
}