package com.chesscoach.backend.analysis;

import com.chesscoach.backend.game.Move;
import com.chesscoach.backend.game.MoveRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GameReportService {

    // Standard, widely-used centipawn-loss bands (same order of magnitude as
    // Lichess/Chess.com's thresholds). These are a deliberate simplification —
    // real engines like Lichess weight loss by *win-probability swing*, not raw
    // centipawns, which behaves better in already-winning/losing positions.
    // Good enough for V1; revisit if reports feel wrong on real games.
    private static final long INACCURACY_THRESHOLD = 50;
    private static final long MISTAKE_THRESHOLD = 100;
    private static final long BLUNDER_THRESHOLD = 300;

    private final MoveRepository moveRepository;
    private final MoveEvaluationRepository moveEvaluationRepository;

    public GameReportService(MoveRepository moveRepository, MoveEvaluationRepository moveEvaluationRepository) {
        this.moveRepository = moveRepository;
        this.moveEvaluationRepository = moveEvaluationRepository;
    }

    public List<MoveReportEntry> buildReport(Long gameId) {
        List<Move> moves = moveRepository.findByGameIdOrderByPlyNumberAsc(gameId);
        if (moves.isEmpty()) {
            return List.of();
        }

        List<MoveEvaluation> evaluations =
                moveEvaluationRepository.findByMove_GameIdOrderByMove_PlyNumberAsc(gameId);
        Map<Long, MoveEvaluation> evalByMoveId = evaluations.stream()
                .collect(Collectors.toMap(e -> e.getMove().getId(), e -> e));

        List<MoveReportEntry> report = new ArrayList<>();
        Long previousMoveId = null;

        for (Move move : moves) {
            MoveEvaluation currentEval = evalByMoveId.get(move.getId());

            if (currentEval == null) {
                report.add(new MoveReportEntry(
                        move.getPlyNumber(), move.getSan(), null, null, null, 0, MoveClassification.PENDING));
                continue;
            }

            MoveEvaluation previousEval = previousMoveId != null ? evalByMoveId.get(previousMoveId) : null;

            // "Before" position = the position the mover was actually facing.
            // That's exactly move N-1's stored evaluation (already from the
            // perspective of whoever moves next, i.e. this move's mover) —
            // no extra Stockfish call needed for N > 1.
            long beforeMoverScore = previousEval != null
                    ? EvaluationScale.toComparable(previousEval.getScoreCentipawns(), previousEval.getMateInMoves())
                    : 0L; // Ply 1 only: approximate the starting position as balanced.
                          // Evaluating the literal starting FEN for real would need one
                          // extra engine call solely for this one edge case per game —
                          // not worth it; "start is ~equal" is an extremely safe assumption.

            // "After" position's stored eval is from the OPPONENT's perspective
            // (they're now to move) — negate to compare on the mover's side.
            long afterMoverScore = EvaluationScale.negate(
                    EvaluationScale.toComparable(currentEval.getScoreCentipawns(), currentEval.getMateInMoves()));

            long loss = Math.max(0, beforeMoverScore - afterMoverScore);
            report.add(new MoveReportEntry(
                    move.getPlyNumber(),
                    move.getSan(),
                    currentEval.getBestMoveUci(),
                    currentEval.getScoreCentipawns(),
                    currentEval.getMateInMoves(),
                    loss,
                    classify(loss)));

            previousMoveId = move.getId();
        }

        return report;
    }

    private MoveClassification classify(long loss) {
        if (loss >= BLUNDER_THRESHOLD) return MoveClassification.BLUNDER;
        if (loss >= MISTAKE_THRESHOLD) return MoveClassification.MISTAKE;
        if (loss >= INACCURACY_THRESHOLD) return MoveClassification.INACCURACY;
        return MoveClassification.NONE;
    }
}