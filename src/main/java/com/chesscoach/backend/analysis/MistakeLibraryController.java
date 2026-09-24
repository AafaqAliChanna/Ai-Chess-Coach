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
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return mistakeLibraryService.buildLibrary(name, parsePhase(phase), limit, offset);
    }

    // This is the actual fix for the 500 the frontend hit: Spring's default
    // binder tries to convert an empty "phase=" query value straight into
    // the GamePhase enum before this method body ever runs, and the
    // resulting MethodArgumentTypeMismatchException falls through
    // GlobalExceptionHandler's catch-all as an unhelpful 500. Taking phase
    // as a raw String and parsing it ourselves means blank/missing = no
    // filter, and only a genuinely unrecognized value becomes a real,
    // explained 400.
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
}