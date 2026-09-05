package com.chesscoach.backend.analysis;

import com.chesscoach.backend.game.GameRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameReportController {

    private final GameRepository gameRepository;
    private final GameReportService gameReportService;

    public GameReportController(GameRepository gameRepository, GameReportService gameReportService) {
        this.gameRepository = gameRepository;
        this.gameReportService = gameReportService;
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<List<MoveReportEntry>> getReport(@PathVariable Long id) {
        if (!gameRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(gameReportService.buildReport(id));
    }
}