package com.chesscoach.backend.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaterialEvaluatorTest {

    @Test
    void startingPositionIsBalanced() {
        String startingFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        assertEquals(0, MaterialEvaluator.materialBalance(startingFen));
    }

    @Test
    void pieceValuesMatchStandardChessValues() {
        assertEquals(1, MaterialEvaluator.pieceValue('P'));
        assertEquals(3, MaterialEvaluator.pieceValue('n'));
        assertEquals(3, MaterialEvaluator.pieceValue('B'));
        assertEquals(5, MaterialEvaluator.pieceValue('r'));
        assertEquals(9, MaterialEvaluator.pieceValue('Q'));
        assertEquals(0, MaterialEvaluator.pieceValue('k')); // kings excluded
        assertEquals(0, MaterialEvaluator.pieceValue('/'));
        assertEquals(0, MaterialEvaluator.pieceValue('8'));
    }

    @Test
    void detectsWhiteMaterialAdvantage() {
        // White has an extra queen, otherwise bare kings.
        String fen = "4k3/8/8/8/8/8/8/4KQ2 w - - 0 1";
        assertEquals(9, MaterialEvaluator.materialBalance(fen));
    }
}