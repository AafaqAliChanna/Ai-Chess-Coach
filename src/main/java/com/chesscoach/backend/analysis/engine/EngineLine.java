package com.chesscoach.backend.analysis.engine;

import java.util.List;

/**
 * One candidate line from a MultiPV search: not just the first move (that's
 * CandidateMove, used by evaluateTopMoves for Brilliant/Great/Good), but
 * the engine's full principal variation for this rank, as reported —
 * length is bounded by, but not guaranteed to equal, the requested search
 * depth.
 */
public record EngineLine(int rank, Integer scoreCentipawns, Integer mateInMoves, List<String> movesUci) {}