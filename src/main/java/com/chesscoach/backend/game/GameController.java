package com.chesscoach.backend.game;

import com.chesscoach.backend.auth.CurrentUserProvider;
import com.chesscoach.backend.auth.ForbiddenException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final GameDeletionService gameDeletionService;
    private final GameIngestionService gameIngestionService;
    private final CurrentUserProvider currentUserProvider;

    public GameController(GameRepository gameRepository,
                           MoveRepository moveRepository,
                           GameDeletionService gameDeletionService,
                           GameIngestionService gameIngestionService,
                           CurrentUserProvider currentUserProvider) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.gameDeletionService = gameDeletionService;
        this.gameIngestionService = gameIngestionService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<Game> uploadGame(@Valid @RequestBody GameUploadRequest request) {
        Long userId = currentUserProvider.requireCurrentUserId();
        Game saved = gameIngestionService.ingest(
                request.pgn(), request.title(), request.whitePlayer(), request.blackPlayer(),
                request.result(), userId, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
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

    private void assertOwnsOrUnowned(Game game) {
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        if (game.getUserId() != null && !game.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to modify this game");
        }
    }
}