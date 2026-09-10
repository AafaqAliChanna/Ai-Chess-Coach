package com.chesscoach.backend.training;

import com.chesscoach.backend.analysis.GamePhase;
import com.chesscoach.backend.analysis.MoveClassification;

public record TrainingExercise(
        Long gameId,
        int plyNumber,
        String fenBefore,      // position the player actually faced
        String playerMove,     // what they played (SAN) — the mistake
        String bestMoveUci,    // what Stockfish says they should have played
        MoveClassification classification,
        GamePhase gamePhase,
        long centipawnLoss
) {}