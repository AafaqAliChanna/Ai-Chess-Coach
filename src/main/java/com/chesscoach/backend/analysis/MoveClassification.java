package com.chesscoach.backend.analysis;

public enum MoveClassification {
    NONE,        // no meaningful evaluation loss
    INACCURACY,
    MISTAKE,
    BLUNDER,
    PENDING      // this ply hasn't been analyzed yet (worker still processing)
}