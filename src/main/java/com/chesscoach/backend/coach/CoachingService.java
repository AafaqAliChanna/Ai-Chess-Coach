package com.chesscoach.backend.coach;

import com.chesscoach.backend.analysis.GameReportService;
import com.chesscoach.backend.analysis.MoveReportEntry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CoachingService {

    private final GameReportService gameReportService;
    private final OllamaClient ollamaClient;

    public CoachingService(GameReportService gameReportService, OllamaClient ollamaClient) {
        this.gameReportService = gameReportService;
        this.ollamaClient = ollamaClient;
    }

    public String generateCoachingSummary(Long gameId) {
        List<MoveReportEntry> report = gameReportService.buildReport(gameId);

        boolean stillAnalyzing = report.stream()
                .anyMatch(e -> e.classification().name().equals("PENDING"));
        if (stillAnalyzing) {
            throw new CoachingException("Game analysis is still in progress — try again in a moment.");
        }

        List<MoveReportEntry> flagged = CoachingPromptBuilder.filterFlaggedMoves(report);
        if (flagged.isEmpty()) {
            return "No significant mistakes found in this game — solid play throughout!";
        }

        String prompt = CoachingPromptBuilder.buildPrompt(flagged);
        return ollamaClient.generate(prompt);
    }
}