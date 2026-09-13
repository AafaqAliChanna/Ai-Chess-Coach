package com.chesscoach.backend.game;

import com.chesscoach.backend.analysis.AnalysisJob;
import com.chesscoach.backend.analysis.AnalysisQueueService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Single place a Game gets created, whether from a manual PGN upload or an
 * external import (Chess.com, later Lichess). Both callers need identical
 * behavior (save, parse moves, enqueue analysis) — extracted here so that
 * behavior can't silently drift between the two entry points.
 */
@Service
public class GameIngestionService {

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final PgnParsingService pgnParsingService;
    private final AnalysisQueueService analysisQueueService;

    public GameIngestionService(GameRepository gameRepository,
                                 MoveRepository moveRepository,
                                 PgnParsingService pgnParsingService,
                                 AnalysisQueueService analysisQueueService) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.pgnParsingService = pgnParsingService;
        this.analysisQueueService = analysisQueueService;
    }

    @Transactional
    public Game ingest(String pgn, String title, String whitePlayer, String blackPlayer,
                        String result, Long userId, String sourceUrl) {
        Game game = new Game();
        game.setPgn(pgn);
        game.setTitle(title);
        game.setWhitePlayer(whitePlayer);
        game.setBlackPlayer(blackPlayer);
        game.setResult(result);
        game.setUserId(userId);
        game.setSourceUrl(sourceUrl);
        Game saved = gameRepository.save(game);

        List<Move> moves = pgnParsingService.parseMoves(saved, pgn);
        moveRepository.saveAll(moves);

        analysisQueueService.enqueueAfterCommit(AnalysisJob.forGame(saved.getId()));
        return saved;
    }
}