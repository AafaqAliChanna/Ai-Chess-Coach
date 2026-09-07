package com.chesscoach.backend.game;

import com.chesscoach.backend.analysis.MoveEvaluation;
import com.chesscoach.backend.analysis.MoveEvaluationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles the full cascade for deleting a game: evaluations -> moves -> game,
 * in that order, done explicitly in application code rather than relying on
 * a database-level ON DELETE CASCADE constraint.
 *
 * Why not just add @OnDelete(CASCADE) to the entities and let Postgres handle it:
 * ddl-auto=update does not reliably retrofit ON DELETE behavior onto a foreign
 * key constraint that already exists in the database (ours does — created back
 * in Phase 2/3). Getting that right would mean manually dropping and re-creating
 * the constraints via SQL. Explicit, ordered deletes here are slower for very
 * large games but completely predictable and don't depend on schema state we'd
 * have to verify by hand.
 */
@Service
public class GameDeletionService {

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final MoveEvaluationRepository moveEvaluationRepository;

    public GameDeletionService(GameRepository gameRepository,
                                MoveRepository moveRepository,
                                MoveEvaluationRepository moveEvaluationRepository) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.moveEvaluationRepository = moveEvaluationRepository;
    }

    @Transactional
    public void deleteGameCascade(Long gameId) {
        List<MoveEvaluation> evaluations =
                moveEvaluationRepository.findByMove_GameIdOrderByMove_PlyNumberAsc(gameId);
        moveEvaluationRepository.deleteAll(evaluations);

        List<Move> moves = moveRepository.findByGameIdOrderByPlyNumberAsc(gameId);
        moveRepository.deleteAll(moves);

        gameRepository.deleteById(gameId);
    }
}