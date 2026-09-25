package com.chesscoach.backend.training;

import com.chesscoach.backend.analysis.GameReportService;
import com.chesscoach.backend.analysis.MoveClassification;
import com.chesscoach.backend.analysis.MoveReportEntry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrainingAttemptService {

    private final GameReportService gameReportService;
    private final TrainingAttemptRepository trainingAttemptRepository;

    public TrainingAttemptService(GameReportService gameReportService,
                                   TrainingAttemptRepository trainingAttemptRepository) {
        this.gameReportService = gameReportService;
        this.trainingAttemptRepository = trainingAttemptRepository;
    }

    public TrainingAttemptResponse recordAttempt(Long userId, TrainingAttemptRequest request) {
        List<MoveReportEntry> report = gameReportService.buildReport(request.gameId());

        MoveReportEntry entry = report.stream()
                .filter(e -> e.plyNumber() == request.plyNumber())
                .findFirst()
                .orElseThrow(() -> new TrainingAttemptException(
                        "No move found at ply " + request.plyNumber() + " for game " + request.gameId()));

        if (entry.classification() == MoveClassification.PENDING) {
            throw new TrainingAttemptException(
                    "Analysis for this game is still in progress — try again in a moment.");
        }

        String correctMoveUci = entry.bestMoveUci();
        if (correctMoveUci == null || correctMoveUci.equals("(none)")) {
            // bestMoveUci comes from the PREVIOUS ply's evaluation (same
            // convention GameReportService itself uses). If that previous
            // position was terminal (checkmate/stalemate), there's genuinely
            // no move to validate against — reject explicitly rather than
            // silently recording a meaningless wasCorrect=false.
            throw new TrainingAttemptException(
                    "No recommended move is available to validate this attempt against.");
        }

        boolean wasCorrect = normalize(request.attemptedMoveUci()).equals(normalize(correctMoveUci));

        TrainingAttempt attempt = new TrainingAttempt();
        attempt.setUserId(userId);
        attempt.setGameId(request.gameId());
        attempt.setPlyNumber(request.plyNumber());
        attempt.setAttemptedMoveUci(request.attemptedMoveUci());
        attempt.setCorrectMoveUci(correctMoveUci);
        attempt.setWasCorrect(wasCorrect);
        TrainingAttempt saved = trainingAttemptRepository.save(attempt);

        return new TrainingAttemptResponse(
                saved.getId(), saved.getGameId(), saved.getPlyNumber(),
                saved.getAttemptedMoveUci(), saved.getCorrectMoveUci(),
                saved.isWasCorrect(), saved.getAttemptedAt());
    }

    // UCI strings from Stockfish are always lowercase, but a hand-typed or
    // frontend-constructed move could plausibly differ in case — trimmed
    // and lowercased before comparing so a genuinely correct answer never
    // fails on formatting alone.
    private String normalize(String uci) {
        return uci == null ? "" : uci.trim().toLowerCase();
    }
}