package com.chesscoach.backend.analysis;

/**
 * Coarse, rule-based classification of WHY a flagged mistake happened,
 * computed entirely from data already stored (this game's own report
 * entries) — no extra Stockfish calls, no LLM. Deliberately NOT a full
 * tactical-motif taxonomy (fork, pin, skewer, discovered attack, etc.) —
 * detecting those reliably requires real board-geometry/attack analysis
 * this project doesn't have yet. This is an honest first pass: mate-related
 * mistakes and "handed over material for free" are both mechanically
 * detectable and reliable from stored evaluations; everything else falls
 * into POSITIONAL rather than being force-fit into a motif we can't
 * actually verify.
 */
public enum PatternTag {
    MISSED_MATE,    // mover had a forced mate available and didn't play it
    ALLOWED_MATE,   // mover's move let the opponent force mate
    HANGING_PIECE,  // opponent's actual next move captured material for free
    POSITIONAL      // flagged as a mistake, but none of the above applied
}