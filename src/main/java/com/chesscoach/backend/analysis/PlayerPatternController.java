package com.chesscoach.backend.analysis;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
public class PlayerPatternController {

    private final PlayerPatternService playerPatternService;
    private final ProgressService progressService;

    public PlayerPatternController(PlayerPatternService playerPatternService, ProgressService progressService) {
        this.playerPatternService = playerPatternService;
        this.progressService = progressService;
    }

    @GetMapping("/{name}/patterns")
    public PlayerPatternSummary getPatterns(@PathVariable String name) {
        return playerPatternService.buildSummary(name);
    }

    @GetMapping("/{name}/progress")
    public ProgressResponse getProgress(@PathVariable String name) {
        return progressService.buildProgress(name);
    }
}