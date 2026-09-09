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
            report.add(new MoveReportEntry(
                    move.getPlyNumber(),
                    move.getSan(),
                    move.getFenAfter(),
                    currentEval.getBestMoveUci(),
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