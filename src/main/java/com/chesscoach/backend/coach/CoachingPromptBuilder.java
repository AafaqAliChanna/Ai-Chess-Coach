package com.chesscoach.backend.coach;

import com.chesscoach.backend.analysis.MoveClassification;
import com.chesscoach.backend.analysis.MoveReportEntry;

import java.util.List;

/**
 * Turns Stockfish's already-computed facts into a grounded prompt.
 * The LLM's job is ONLY to explain these facts in plain language —
 * per the AI guardrail principle from the project blueprint, it is
 * never asked to evaluate a position or suggest a move itself.
 */
public class CoachingPromptBuilder {

    private CoachingPromptBuilder() {}

    public static String buildPrompt(List<MoveReportEntry> flaggedMoves) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a chess coach. Below are specific mistakes a player made in a game, ")
          .append("already identified by a chess engine (Stockfish). For each one, explain in ")
          .append("plain, encouraging language why it was a mistake and what the better move ")
          .append("achieves instead. Do NOT invent moves, evaluations, or variations beyond what ")
          .append("is given below — only explain the facts provided. Keep each explanation to 2-3 ")
          .append("sentences. Number your explanations to match the move numbers given.\n\n");

        for (MoveReportEntry entry : flaggedMoves) {
            sb.append(String.format(
                    "Move %d: player played %s (classified as %s, lost %d centipawns). " +
                    "The engine's recommended move instead was: %s.%n",
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