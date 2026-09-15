package com.chesscoach.backend.coach;

import com.chesscoach.backend.analysis.MoveClassification;
import com.chesscoach.backend.analysis.MoveReportEntry;

import java.util.List;

public class CoachingPromptBuilder {

    private CoachingPromptBuilder() {}

    public static String buildPrompt(List<MoveReportEntry> flaggedMoves) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a chess coach. Below are specific mistakes a player made in a game, ")
          .append("already identified by a chess engine (Stockfish). Respond with STRUCTURED JSON ")
          .append("ONLY \u2014 no markdown code fences, no text before or after the JSON.\n\n")
          .append("Respond with EXACTLY this JSON shape:\n")
          .append("{\n")
          .append("  \"strengths\": [\"short observation\", ...],\n")
          .append("  \"weaknesses\": [\"short observation\", ...],\n")
          .append("  \"keyMoments\": [ {\"plyNumber\": <number from the list below>, ")
          .append("\"comment\": \"2-3 sentence explanation of why this specific move was a mistake ")
          .append("and what the recommended move achieves instead\"}, ... ],\n")
          .append("  \"recommendation\": \"1-2 sentence overall suggestion for what to practice next\"\n")
          .append("}\n\n")
          .append("Rules:\n")
          .append("- Do NOT invent moves, evaluations, or variations beyond what is given below.\n")
          .append("- Include a keyMoments entry for EVERY move listed below, using its exact plyNumber.\n")
          .append("- strengths/weaknesses should be brief, general observations (1-4 items each).\n\n")
          .append("Mistakes to analyze:\n");

        for (MoveReportEntry entry : flaggedMoves) {
            sb.append(String.format(
                    "Ply %d: player played %s (classified as %s, lost %d centipawns). " +
                    "Engine's recommended move instead: %s.%n",
                    entry.plyNumber(), entry.san(), entry.classification(),
                    entry.centipawnLoss(), entry.bestMoveUci()));
        }

        return sb.toString();
    }

    public static List<MoveReportEntry> filterFlaggedMoves(List<MoveReportEntry> fullReport) {
        return fullReport.stream()
                .filter(e -> e.classification() == MoveClassification.INACCURACY
                        || e.classification() == MoveClassification.MISTAKE
                        || e.classification() == MoveClassification.BLUNDER)
                .toList();
    }
}