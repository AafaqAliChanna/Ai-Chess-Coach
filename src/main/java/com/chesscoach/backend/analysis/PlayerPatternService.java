package com.chesscoach.backend.analysis;

import com.chesscoach.backend.game.Game;
import com.chesscoach.backend.game.GameRepository;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class PlayerPatternService {

    private final GameRepository gameRepository;
    private final GameReportService gameReportService;

    public PlayerPatternService(GameRepository gameRepository, GameReportService gameReportService) {
        this.gameRepository = gameRepository;
        this.gameReportService = gameReportService;
    }

    public PlayerPatternSummary buildSummary(String playerName) {
        List<Game> games = gameRepository.findByWhitePlayerIgnoreCaseOrBlackPlayerIgnoreCase(playerName, playerName);

        Map<GamePhase, Map<MoveClassification, Integer>> tally = new EnumMap<>(GamePhase.class);
        for (GamePhase phase : GamePhase.values()) {
            tally.put(phase, new EnumMap<>(MoveClassification.class));
        }

        int gamesAnalyzed = 0;

        for (Game game : games) {
            boolean playerIsWhite = game.getWhitePlayer() != null
                    && game.getWhitePlayer().equalsIgnoreCase(playerName);

            List<MoveReportEntry> report = gameReportService.buildReport(game.getId());
            if (report.isEmpty()) continue;

            boolean stillPending = report.stream()
                    .anyMatch(e -> e.classification() == MoveClassification.PENDING);
            if (stillPending) continue;

            gamesAnalyzed++;

            for (MoveReportEntry entry : report) {
                boolean thisMoveWasPlayers = (entry.plyNumber() % 2 == 1) == playerIsWhite;
                if (!thisMoveWasPlayers) continue;
                if (entry.classification() == MoveClassification.NONE) continue;

                tally.get(entry.gamePhase()).merge(entry.classification(), 1, Integer::sum);
            }
        }

        return new PlayerPatternSummary(playerName, games.size(), gamesAnalyzed, tally);
    }
}