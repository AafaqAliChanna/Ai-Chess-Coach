package com.chesscoach.backend.training;

import com.chesscoach.backend.analysis.GamePhase;
import com.chesscoach.backend.analysis.GameReportService;
import com.chesscoach.backend.analysis.MoveClassification;
import com.chesscoach.backend.analysis.MoveReportEntry;
import com.chesscoach.backend.game.Game;
import com.chesscoach.backend.game.GameRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TrainingService {

    private static final String STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private final GameRepository gameRepository;
    private final GameReportService gameReportService;

    public TrainingService(GameRepository gameRepository, GameReportService gameReportService) {
        this.gameRepository = gameRepository;
        this.gameReportService = gameReportService;
    }

    public List<TrainingExercise> buildExercises(String playerName, int limit, GamePhase phaseFilter) {
        List<Game> games = gameRepository.findByWhitePlayerIgnoreCaseOrBlackPlayerIgnoreCase(playerName, playerName);
        List<TrainingExercise> candidates = new ArrayList<>();

        for (Game game : games) {
            boolean playerIsWhite = game.getWhitePlayer() != null
                    && game.getWhitePlayer().equalsIgnoreCase(playerName);

            List<MoveReportEntry> report = gameReportService.buildReport(game.getId());
            if (report.isEmpty()) continue;

            boolean stillPending = report.stream()
                    .anyMatch(e -> e.classification() == MoveClassification.PENDING);
            if (stillPending) continue;

            for (int i = 0; i < report.size(); i++) {
                MoveReportEntry entry = report.get(i);

                boolean thisMoveWasPlayers = (entry.plyNumber() % 2 == 1) == playerIsWhite;
                if (!thisMoveWasPlayers) continue;
                if (entry.classification() == MoveClassification.NONE) continue;
                if (phaseFilter != null && entry.gamePhase() != phaseFilter) continue;

                String fenBefore = (i == 0) ? STARTING_FEN : report.get(i - 1).fenAfter();

                candidates.add(new TrainingExercise(
                        game.getId(),
                        entry.plyNumber(),
                        fenBefore,
                        entry.san(),
                        entry.bestMoveUci(),
                        entry.classification(),
                        entry.gamePhase(),
                        entry.centipawnLoss(),
                        entry.winPercentLoss()));
            }
        }

        // Sorted by win-probability loss now, not raw centipawn loss \u2014 a
        // "worst mistake" ranking should reflect genuine practical severity,
        // the entire point of this whole upgrade.
        return candidates.stream()
                .sorted(Comparator.comparingDouble(TrainingExercise::winPercentLoss).reversed())
                .limit(limit)
                .toList();
    }
}