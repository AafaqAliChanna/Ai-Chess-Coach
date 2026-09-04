package com.chesscoach.backend.analysis.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class StockfishEngineTest {

    // No Stockfish path is hardcoded/committed here on purpose — it's your
    // machine's local path. Pass it via -Dstockfish.path=... when running.
    // If it's not provided, the test skips instead of failing, so this
    // doesn't break the build on a machine/CI without Stockfish installed.
    private static final String STOCKFISH_PATH = System.getProperty("stockfish.path");

    @Test
    void evaluatesStartingPosition() {
        assumeTrue(STOCKFISH_PATH != null, "Skipping: pass -Dstockfish.path=<path> to run this test");

        try (StockfishEngine engine = new StockfishEngine(STOCKFISH_PATH)) {
            engine.start();
            assertTrue(engine.isAlive(), "Engine should be running after start()");

            String startingFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
            EngineEvaluation eval = engine.evaluate(startingFen, 10);

            assertNotNull(eval.bestMoveUci(), "Should return a best move for the starting position");
            assertFalse(eval.isMate(), "Starting position should never be a forced mate");
            assertNotNull(eval.scoreCentipawns(), "Starting position should have a centipawn score");

            // Starting position is close to equal — sanity-check the eval isn't wildly broken.
            assertTrue(Math.abs(eval.scoreCentipawns()) < 150,
                    "Starting position eval should be near 0, got: " + eval.scoreCentipawns());
        }
    }

    @Test
    void detectsForcedMate() {
        assumeTrue(STOCKFISH_PATH != null, "Skipping: pass -Dstockfish.path=<path> to run this test");

        // Fool's mate position — Black has mate-in-1 available (Qh4#).
        String mateInOneFen = "rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq - 0 2";

        try (StockfishEngine engine = new StockfishEngine(STOCKFISH_PATH)) {
            engine.start();
            EngineEvaluation eval = engine.evaluate(mateInOneFen, 10);

            assertTrue(eval.isMate(), "Should detect forced mate, got centipawn score instead");
            assertEquals(1, Math.abs(eval.mateInMoves()), "Should be mate in exactly 1");
        }
    }
}