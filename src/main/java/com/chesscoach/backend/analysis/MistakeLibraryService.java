package com.chesscoach.backend.analysis;

import com.chesscoach.backend.game.Game;
import com.chesscoach.backend.game.GameRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MistakeLibraryService {

    private static final String STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private final GameRepository gameRepository;
    private final GameReportService gameReportService;

    public MistakeLibraryService(GameRepository gameRepository, GameReportService gameReportService) {
        this.gameRepository = gameRepository;
        this.gameReportService = gameReportService;
    }

    public MistakeLibraryResponse buildLibrary(String playerName, GamePhase phaseFilter, int limit, int offset) {
        List<Game> games = gameRepository.findByWhitePlayerIgnoreCaseOrBlackPlayerIgnoreCase(playerName, playerName);

        List<MistakeLibraryEntry> allMistakes = new ArrayList<>();

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

                allMistakes.add(new MistakeLibraryEntry(
                        game.getId(),
                        game.getTitle(),
                        game.getUploadedAt(),
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

        // Worst mistakes first, same severity-first convention TrainingService
        // already uses — an assumption, not a locked-in contract. See the
        // handoff note: easy to flip to chronological (Comparator.comparing
        // on uploadedAt) if the frontend actually wants a straight timeline.
        allMistakes.sort(Comparator.comparingDouble(MistakeLibraryEntry::winPercentLoss).reversed());

        int total = allMistakes.size();
        List<MistakeLibraryEntry> page = paginate(allMistakes, limit, Math.max(0, offset));

        return new MistakeLibraryResponse(playerName, total, limit, offset, page);
    }

    private List<MistakeLibraryEntry> paginate(List<MistakeLibraryEntry> all, int limit, int offset) {
        if (offset >= all.size() || limit <= 0) {
            return List.of();
        }
        int end = Math.min(offset + limit, all.size());
        return all.subList(offset, end);
    }
}