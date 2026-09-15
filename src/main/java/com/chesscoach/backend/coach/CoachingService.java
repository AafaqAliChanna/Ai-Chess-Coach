package com.chesscoach.backend.coach;

import com.chesscoach.backend.analysis.GameReportService;
import com.chesscoach.backend.analysis.MoveReportEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CoachingService {

    private static final Logger log = LoggerFactory.getLogger(CoachingService.class);

    private final GameReportService gameReportService;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public CoachingService(GameReportService gameReportService, LlmClient llmClient, ObjectMapper objectMapper) {
        this.gameReportService = gameReportService;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public CoachingSummary generateCoachingSummary(Long gameId) {
        List<MoveReportEntry> report = gameReportService.buildReport(gameId);

        boolean stillAnalyzing = report.stream()
                .anyMatch(e -> e.classification().name().equals("PENDING"));
        if (stillAnalyzing) {
            throw new CoachingException("Game analysis is still in progress \u2014 try again in a moment.");
        }

        List<MoveReportEntry> flagged = CoachingPromptBuilder.filterFlaggedMoves(report);
        if (flagged.isEmpty()) {
            return new CoachingSummary(
                    List.of("Solid, consistent play with no significant mistakes flagged"),
                    List.of(),
                    List.of(),
                    "Keep playing at this level \u2014 nothing specific to correct from this game.");
        }

        String rawResponse = llmClient.generate(CoachingPromptBuilder.buildPrompt(flagged));
        return parseStructuredResponse(rawResponse);
    }

    private CoachingSummary parseStructuredResponse(String rawResponse) {
        // LLMs frequently wrap JSON in ```json fences even when explicitly
        // told not to \u2014 stripped defensively rather than trusting
        // instruction-following alone.
        String cleaned = rawResponse.trim()
                .replaceAll("^```json\\s*", "")
                .replaceAll("^```\\s*", "")
                .replaceAll("```\\s*$", "")
                .trim();

        try {
            return objectMapper.readValue(cleaned, CoachingSummary.class);
        } catch (Exception e) {
            log.warn("Failed to parse LLM response as structured JSON. Raw response: {}", rawResponse);
            throw new CoachingException("The AI coach's response could not be parsed as structured data. Try again.", e);
        }
    }
}