package com.chesscoach.backend.analysis;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
public class PlayerPatternController {

    private final PlayerPatternService playerPatternService;

    public PlayerPatternController(PlayerPatternService playerPatternService) {
        this.playerPatternService = playerPatternService;
    }

    @GetMapping("/{name}/patterns")
    public PlayerPatternSummary getPatterns(@PathVariable String name) {
        return playerPatternService.buildSummary(name);
    }
}