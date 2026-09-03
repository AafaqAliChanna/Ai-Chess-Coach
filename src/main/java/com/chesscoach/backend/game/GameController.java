package com.chesscoach.backend.game;

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

    public GameController(GameRepository gameRepository,
                           MoveRepository moveRepository,
                           PgnParsingService pgnParsingService) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.pgnParsingService = pgnParsingService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Game> uploadGame(@Valid @RequestBody GameUploadRequest request) {
        Game game = new Game();
        game.setPgn(request.pgn());
        game.setWhitePlayer(request.whitePlayer());
        game.setBlackPlayer(request.blackPlayer());
        game.setResult(request.result());
        Game savedGame = gameRepository.save(game);

        List<Move> moves = pgnParsingService.parseMoves(savedGame, request.pgn());
        moveRepository.saveAll(moves);

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
}