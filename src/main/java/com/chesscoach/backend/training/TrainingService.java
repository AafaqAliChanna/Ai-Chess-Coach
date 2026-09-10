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

    // Standard chess starting position — used as fenBefore only for ply 1,
    // where there is no "previous move" to derive it from.
    private static final String STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private final GameRepository gameRepository;
    private final GameReportService gameReportService;

    public TrainingService(GameRepository gameRepository, GameReportService gameReportService) {
        this.gameRepository = gameRepository;
        this.gameReportService = gameReportService;
    }

    /**
     * Builds up to `limit` training exercises from a player's own real
     * mistakes, worst (highest centipawn loss) first. Optional phaseFilter
     * narrows to one phase — e.g. "give me only endgame exercises" — since
     * the pattern engine (Phase 7) might tell a player that's their weakest
     * area specifically.
     */
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

                // fenBefore = previous ply's fenAfter (same game, same report list,
                // already in memory) — or the starting position for ply 1.
                String fenBefore = (i == 0) ? STARTING_FEN : report.get(i - 1).fenAfter();

                candidates.add(new TrainingExercise(
                        game.getId(),
                        entry.plyNumber(),
                        fenBefore,
                        entry.san(),
                        entry.bestMoveUci(),
                        entry.classification(),
                        entry.gamePhase(),
                        entry.centipawnLoss()));
            }
        }

        return candidates.stream()
                .sorted(Comparator.comparingLong(TrainingExercise::centipawnLoss).reversed())
                .limit(limit)
                .toList();
    }
}