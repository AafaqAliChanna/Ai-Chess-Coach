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
            GamePhase phase = GamePhase.fromFen(move.getFenAfter(), move.getPlyNumber());

            if (currentEval == null) {
                report.add(new MoveReportEntry(
                        move.getPlyNumber(), move.getSan(), move.getFenAfter(),
                        null, null, null, 0, MoveClassification.PENDING, phase));
                continue;
            }

            MoveEvaluation previousEval = previousMoveId != null ? evalByMoveId.get(previousMoveId) : null;

            long beforeMoverScore = previousEval != null
                    ? EvaluationScale.toComparable(previousEval.getScoreCentipawns(), previousEval.getMateInMoves())
                    : 0L;

            long afterMoverScore = EvaluationScale.negate(
                    EvaluationScale.toComparable(currentEval.getScoreCentipawns(), currentEval.getMateInMoves()));

            long loss = Math.max(0, beforeMoverScore - afterMoverScore);

            // FIX: bestMoveUci must reflect what the MOVER should have played,
            // i.e. Stockfish's recommendation for the position they actually
            // faced (previousEval) — not currentEval, which is the engine's
            // recommendation for the OPPONENT'S reply to the move just played.
            // null for ply 1: no evaluation of the literal starting position
            // exists (same documented gap as beforeMoverScore's 0L default above).
            String bestMoveUci = previousEval != null ? previousEval.getBestMoveUci() : null;

            report.add(new MoveReportEntry(
                    move.getPlyNumber(),
                    move.getSan(),
                    move.getFenAfter(),
                    bestMoveUci,
                    currentEval.getScoreCentipawns(),
                    currentEval.getMateInMoves(),
                    loss,
                    classify(loss),
                    phase));

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