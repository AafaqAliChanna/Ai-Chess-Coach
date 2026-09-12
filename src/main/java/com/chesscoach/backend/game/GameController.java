package com.chesscoach.backend.game;

import com.chesscoach.backend.analysis.AnalysisJob;
import com.chesscoach.backend.analysis.AnalysisQueueService;
import com.chesscoach.backend.auth.CurrentUserProvider;
import com.chesscoach.backend.auth.ForbiddenException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final PgnParsingService pgnParsingService;
    private final AnalysisQueueService analysisQueueService;
    private final GameDeletionService gameDeletionService;
    private final CurrentUserProvider currentUserProvider;

    public GameController(GameRepository gameRepository,
                           MoveRepository moveRepository,
                           PgnParsingService pgnParsingService,
                           AnalysisQueueService analysisQueueService,
                           GameDeletionService gameDeletionService,
                           CurrentUserProvider currentUserProvider) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.pgnParsingService = pgnParsingService;
        this.analysisQueueService = analysisQueueService;
        this.gameDeletionService = gameDeletionService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Game> uploadGame(@Valid @RequestBody GameUploadRequest request) {
        Long userId = currentUserProvider.requireCurrentUserId();

        Game game = new Game();
        game.setPgn(request.pgn());
        game.setTitle(request.title());
        game.setWhitePlayer(request.whitePlayer());
        game.setBlackPlayer(request.blackPlayer());
        game.setResult(request.result());
        game.setUserId(userId);
        Game savedGame = gameRepository.save(game);

        List<Move> moves = pgnParsingService.parseMoves(savedGame, request.pgn());
        moveRepository.saveAll(moves);

        analysisQueueService.enqueueAfterCommit(AnalysisJob.forGame(savedGame.getId()));

        return ResponseEntity.status(HttpStatus.CREATED).body(savedGame);
    }

    @GetMapping
    public List<Game> listGames() {
        return gameRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Game> getGame(@PathVariable Long id) {
        return gameRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/moves")
    public ResponseEntity<List<Move>> getGameMoves(@PathVariable Long id) {
        if (!gameRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(moveRepository.findByGameIdOrderByPlyNumberAsc(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable Long id) {
        Game game = gameRepository.findById(id).orElse(null);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }
        assertOwnsOrUnowned(game);
        gameDeletionService.deleteGameCascade(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Game> updateGameTitle(@PathVariable Long id,
                                                 @RequestBody GameTitleUpdateRequest request) {
        return gameRepository.findById(id)
                .map(game -> {
                    assertOwnsOrUnowned(game);
                    game.setTitle(request.title());
                    Game saved = gameRepository.save(game);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Games uploaded before accounts existed have userId == null. Treated as
    // unowned/editable by any authenticated user for now — a deliberate
    // compatibility choice for existing test data, not an oversight. Once
    // every game has a real owner, this null-check branch should be removed.
    private void assertOwnsOrUnowned(Game game) {
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        if (game.getUserId() != null && !game.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to modify this game");
        }
    }
}