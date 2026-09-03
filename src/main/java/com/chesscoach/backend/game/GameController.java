package com.chesscoach.backend.game;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameRepository gameRepository;

    public GameController(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @PostMapping
    public ResponseEntity<Game> uploadGame(@Valid @RequestBody GameUploadRequest request) {
        Game game = new Game();
        game.setPgn(request.pgn());
        game.setWhitePlayer(request.whitePlayer());
        game.setBlackPlayer(request.blackPlayer());
        game.setResult(request.result());

        Game saved = gameRepository.save(game);
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
}