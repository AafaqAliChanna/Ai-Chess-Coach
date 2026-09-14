
package com.chesscoach.backend.analysis;

import com.chesscoach.backend.game.Game;
import com.chesscoach.backend.game.GameRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ProgressService {

    private final GameRepository gameRepository;
    private final GameReportService gameReportService;

    public ProgressService(GameRepository gameRepository, GameReportService gameReportService) {
        this.gameRepository = gameRepository;
        this.gameReportService = gameReportService;
    }

    public ProgressResponse buildProgress(String playerName) {
        List<Game> games = gameRepository.findByWhitePlayerIgnoreCaseOrBlackPlayerIgnoreCase(playerName, playerName);

        List<ProgressEntry> entries = new ArrayList<>();
        int stillAnalyzing = 0;

        for (Game game : games) {
            boolean playerIsWhite = game.getWhitePlayer() != null
                    && game.getWhitePlayer().equalsIgnoreCase(playerName);

            List<MoveReportEntry> report = gameReportService.buildReport(game.getId());
            if (report.isEmpty()) continue;

            boolean pending = report.stream().anyMatch(e -> e.classification() == MoveClassification.PENDING);
            if (pending) {
                stillAnalyzing++;
                continue;
            }

            Map<MoveClassification, Integer> counts = new EnumMap<>(MoveClassification.class);
            int totalMoves = 0;

            for (MoveReportEntry entry : report) {
                boolean thisMoveWasPlayers = (entry.plyNumber() % 2 == 1) == playerIsWhite;
                if (!thisMoveWasPlayers) continue;
                totalMoves++;
                if (entry.classification() == MoveClassification.NONE) continue;
                counts.merge(entry.classification(), 1, Integer::sum);
            }

            entries.add(new ProgressEntry(game.getId(), game.getTitle(), game.getUploadedAt(), totalMoves, counts));
        }

        // Chronological ascending — the whole point is a trend line over time,
        // not a list sorted arbitrarily.
        entries.sort(Comparator.comparing(ProgressEntry::uploadedAt));

        return new ProgressResponse(playerName, entries.size(), stillAnalyzing, entries);
    }
}