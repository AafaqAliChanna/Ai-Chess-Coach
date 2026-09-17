package com.chesscoach.backend.training;

import com.chesscoach.backend.analysis.GamePhase;
import com.chesscoach.backend.analysis.MoveClassification;

public record TrainingExercise(
        Long gameId,
        int plyNumber,
        String fenBefore,
        String playerMove,
        String bestMoveUci,
        MoveClassification classification,
        GamePhase gamePhase,
        long centipawnLoss,
        double winPercentLoss
) {}