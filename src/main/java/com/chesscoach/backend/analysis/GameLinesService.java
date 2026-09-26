package com.chesscoach.backend.analysis;

import com.chesscoach.backend.analysis.engine.EngineLine;
import com.chesscoach.backend.analysis.engine.StockfishEngine;
import com.chesscoach.backend.analysis.engine.StockfishEnginePool;
import com.chesscoach.backend.game.Move;
import com.chesscoach.backend.game.MoveRepository;
import com.github.bhlangonijr.chesslib.move.MoveConversionException;
import com.github.bhlangonijr.chesslib.move.MoveList;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class GameLinesService {

    private static final String STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    // Bounded, same "fail loud rather than hang the request" principle as
    // AnalysisWorker's BORROW_TIMEOUT — this runs synchronously in an HTTP
    // request thread and shares the same engine pool as the background
    // analysis worker, so it must not wait indefinitely under pool
    // contention. Real tradeoff worth watching: under heavy background
    // analysis load, a request here can legitimately wait up to this long
    // just to get an engine, before any actual search time.
    private static final long BORROW_TIMEOUT_SECONDS = 15;

    private final MoveRepository moveRepository;
    private final StockfishEnginePool enginePool;

    public GameLinesService(MoveRepository moveRepository, StockfishEnginePool enginePool) {
        this.moveRepository = moveRepository;
        this.enginePool = enginePool;
    }

    public PracticeLinesResponse buildLines(Long gameId, int plyNumber, int numLines, int depthPly) {
        // Deliberately reads Move directly rather than going through
        // GameReportService — this endpoint's only real dependency is the
        // position itself (parsed at upload time), not Stockfish's
        // background analysis status. Unlike /report, /training, and
        // /mistake-library, this works even on a game still queued for
        // analysis.
        List<Move> moves = moveRepository.findByGameIdOrderByPlyNumberAsc(gameId);

        int index = -1;
        for (int i = 0; i < moves.size(); i++) {
            if (moves.get(i).getPlyNumber() == plyNumber) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            throw new GameLinesException("No move found at ply " + plyNumber + " for game " + gameId);
        }

        String fenBefore = (index == 0) ? STARTING_FEN : moves.get(index - 1).getFenAfter();

        StockfishEngine engine = enginePool.borrow(BORROW_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        try {
            List<EngineLine> engineLines = engine.evaluateLines(fenBefore, depthPly, numLines);
            List<PracticeLine> lines = new ArrayList<>();
            for (EngineLine line : engineLines) {
                lines.add(toPracticeLine(fenBefore, line));
            }
            return new PracticeLinesResponse(lines);
        } finally {
            enginePool.release(engine);
        }
    }

    private PracticeLine toPracticeLine(String fenBefore, EngineLine engineLine) {
        try {
            MoveList moveList = new MoveList(fenBefore);
            moveList.loadFromText(String.join(" ", engineLine.movesUci()));
            String[] sanArray = moveList.toSanArray();
            String fenAfterFirstMove = moveList.getFen(1);

            return new PracticeLine(
                    engineLine.scoreCentipawns(),
                    engineLine.mateInMoves(),
                    engineLine.movesUci(),
                    List.of(sanArray),
                    fenAfterFirstMove);
        } catch (MoveConversionException e) {
            // A PV move chesslib can't replay/convert is a real bug worth
            // surfacing loudly, not silently dropping the line — same
            // "fail clear, don't guess" principle as everywhere else here.
            throw new GameLinesException(
                    "Failed to convert engine line to SAN for position " + fenBefore + ": " + e.getMessage());
        }
    }
}