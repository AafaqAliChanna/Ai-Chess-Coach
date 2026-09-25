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
public class MistakeLibraryService {

    private static final String STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private final GameRepository gameRepository;
    private final GameReportService gameReportService;

    public MistakeLibraryService(GameRepository gameRepository, GameReportService gameReportService) {
        this.gameRepository = gameRepository;
        this.gameReportService = gameReportService;
    }

    public MistakeLibraryResponse buildLibrary(String playerName, GamePhase phaseFilter,
                                                PatternTag patternFilter, int limit, int offset) {
        List<Game> games = gameRepository.findByWhitePlayerIgnoreCaseOrBlackPlayerIgnoreCase(playerName, playerName);

        List<MistakeLibraryEntry> matchingPhase = new ArrayList<>();

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
                PatternTag tag = PatternDetector.classify(report, i, playerIsWhite);

                matchingPhase.add(new MistakeLibraryEntry(
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
                        entry.winPercentLoss(),
                        tag));
            }
        }

        // Counts computed from the phase-filtered set BEFORE the pattern
        // filter is applied — these drive filter-UI tab counts, which
        // should show what's available to switch TO, not shrink to match
        // whichever pattern happens to be selected right now.
        Map<PatternTag, Integer> patternCounts = new EnumMap<>(PatternTag.class);
        for (PatternTag tag : PatternTag.values()) {
            patternCounts.put(tag, 0);
        }
        for (MistakeLibraryEntry entry : matchingPhase) {
            patternCounts.merge(entry.patternTag(), 1, Integer::sum);
        }

        List<MistakeLibraryEntry> matching = (patternFilter == null)
                ? matchingPhase
                : matchingPhase.stream().filter(e -> e.patternTag() == patternFilter).toList();

        List<MistakeLibraryEntry> sorted = new ArrayList<>(matching);
        sorted.sort(Comparator.comparingDouble(MistakeLibraryEntry::winPercentLoss).reversed());

        int total = sorted.size();
        List<MistakeLibraryEntry> page = paginate(sorted, limit, Math.max(0, offset));

        return new MistakeLibraryResponse(playerName, total, limit, offset, patternCounts, page);
    }

    private List<MistakeLibraryEntry> paginate(List<MistakeLibraryEntry> all, int limit, int offset) {
        if (offset >= all.size() || limit <= 0) {
            return List.of();
        }
        int end = Math.min(offset + limit, all.size());
        return all.subList(offset, end);
    }
}