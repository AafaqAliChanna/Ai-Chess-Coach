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

    // Thresholds in WIN-PROBABILITY PERCENTAGE POINTS DROPPED, not raw
    // centipawns \u2014 confirmed from Lichess's own published source
    // (lila/modules/analyse/src/main/Advice.scala): a drop of >=30 points
    // is a Blunder, >=20 a Mistake, >=10 an Inaccuracy. This is the actual
    // fix for the old "raw cp diff" distortion (see WinProbability's
    // Javadoc for why that mattered).
    private static final double BLUNDER_THRESHOLD = 30.0;
    private static final double MISTAKE_THRESHOLD = 20.0;
    private static final double INACCURACY_THRESHOLD = 10.0;

    // Raw centipawnLoss is kept as real underlying data (not removed), but
    // display values are capped at a sane maximum \u2014 this is the fix for
    // the previously-known "999032"-style nonsense numbers that occurred
    // when a mate score was involved (EvaluationScale's internal huge-offset
    // trick, meant only for internal comparison, was leaking into a display
    // field). 2000cp (~2 queens' worth) is a conventional "clearly decisive,
    // exact number no longer meaningful" cap.
    private static final long MAX_DISPLAYED_CENTIPAWN_LOSS = 2000;

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
                        null, null, null, 0, 0.0, MoveClassification.PENDING, phase));
                continue;
            }

            MoveEvaluation previousEval = previousMoveId != null ? evalByMoveId.get(previousMoveId) : null;

            // --- Raw centipawn loss (kept as real data, display-capped) ---
            long beforeMoverScore = previousEval != null
                    ? EvaluationScale.toComparable(previousEval.getScoreCentipawns(), previousEval.getMateInMoves())
                    : 0L;
            long afterMoverScore = EvaluationScale.negate(
                    EvaluationScale.toComparable(currentEval.getScoreCentipawns(), currentEval.getMateInMoves()));
            long rawLoss = Math.max(0, beforeMoverScore - afterMoverScore);
            long displayedCentipawnLoss = Math.min(rawLoss, MAX_DISPLAYED_CENTIPAWN_LOSS);

            // --- Win-probability loss (the metric classification now uses) ---
            double beforeMoverWinProb = (previousEval != null)
                    ? WinProbability.fromEvaluation(previousEval.getScoreCentipawns(), previousEval.getMateInMoves())
                    : 50.0; // ply 1: no stored eval of the literal starting position \u2014 same
                            // documented approximation as before, just expressed in win% terms
                            // (0 raw cp and 50% win probability represent the identical assumption).
            // currentEval is from the OPPONENT'S perspective (they're now to move);
            // "my" win probability is the complement, mirroring how the raw-cp path
            // uses EvaluationScale.negate() for the same perspective flip.
            double afterMoverWinProb = 100.0 - WinProbability.fromEvaluation(
                    currentEval.getScoreCentipawns(), currentEval.getMateInMoves());
            double winPercentLoss = Math.max(0.0, beforeMoverWinProb - afterMoverWinProb);

            String bestMoveUci = previousEval != null ? previousEval.getBestMoveUci() : null;

            report.add(new MoveReportEntry(
                    move.getPlyNumber(),
                    move.getSan(),
                    move.getFenAfter(),
                    bestMoveUci,
                    currentEval.getScoreCentipawns(),
                    currentEval.getMateInMoves(),
                    displayedCentipawnLoss,
                    winPercentLoss,
                    classify(winPercentLoss),
                    phase));

            previousMoveId = move.getId();
        }

        return report;
    }

    private MoveClassification classify(double winPercentLoss) {
        if (winPercentLoss >= BLUNDER_THRESHOLD) return MoveClassification.BLUNDER;
        if (winPercentLoss >= MISTAKE_THRESHOLD) return MoveClassification.MISTAKE;
        if (winPercentLoss >= INACCURACY_THRESHOLD) return MoveClassification.INACCURACY;
        return MoveClassification.NONE;
    }
}