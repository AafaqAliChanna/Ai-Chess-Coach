package com.chesscoach.backend.analysis;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/players")
public class MistakeLibraryController {

    private final MistakeLibraryService mistakeLibraryService;

    public MistakeLibraryController(MistakeLibraryService mistakeLibraryService) {
        this.mistakeLibraryService = mistakeLibraryService;
    }

    @GetMapping("/{name}/mistake-library")
    public MistakeLibraryResponse getMistakeLibrary(
            @PathVariable String name,
            @RequestParam(required = false) String phase,
            @RequestParam(required = false) String pattern,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return mistakeLibraryService.buildLibrary(
                name, parsePhase(phase), parsePattern(pattern), limit, offset);
    }

    private GamePhase parsePhase(String phase) {
        if (phase == null || phase.isBlank()) {
            return null;
        }
        try {
            return GamePhase.valueOf(phase.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown game phase \"" + phase + "\". Valid values: OPENING, MIDDLEGAME, ENDGAME.");
        }
    }

    private PatternTag parsePattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return null;
        }
        try {
            return PatternTag.valueOf(pattern.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown pattern tag \"" + pattern + "\". Valid values: MISSED_MATE, ALLOWED_MATE, HANGING_PIECE, POSITIONAL.");
        }
    }
}