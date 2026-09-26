package com.chesscoach.backend.analysis;

import com.chesscoach.backend.game.GameRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/games")
public class GameLinesController {

    private static final int MIN_LINES = 1;
    private static final int MAX_LINES = 5;
    private static final int MIN_DEPTH = 4;
    private static final int MAX_DEPTH = 16;

    private final GameRepository gameRepository;
    private final GameLinesService gameLinesService;

    public GameLinesController(GameRepository gameRepository, GameLinesService gameLinesService) {
        this.gameRepository = gameRepository;
        this.gameLinesService = gameLinesService;
    }

    @GetMapping("/{gameId}/moves/{plyNumber}/lines")
    public ResponseEntity<PracticeLinesResponse> getLines(
            @PathVariable Long gameId,
            @PathVariable int plyNumber,
            @RequestParam(defaultValue = "1") int lines,
            @RequestParam(defaultValue = "6") int depthPly) {

        if (!gameRepository.existsById(gameId)) {
            return ResponseEntity.notFound().build();
        }
        if (lines < MIN_LINES || lines > MAX_LINES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "lines must be between " + MIN_LINES + " and " + MAX_LINES + ".");
        }
        if (depthPly < MIN_DEPTH || depthPly > MAX_DEPTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "depthPly must be between " + MIN_DEPTH + " and " + MAX_DEPTH + ".");
        }

        return ResponseEntity.ok(gameLinesService.buildLines(gameId, plyNumber, lines, depthPly));
    }
}