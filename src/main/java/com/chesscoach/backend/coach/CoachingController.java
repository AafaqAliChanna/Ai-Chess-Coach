package com.chesscoach.backend.coach;

import com.chesscoach.backend.game.GameRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class CoachingController {

    private final GameRepository gameRepository;
    private final CoachingService coachingService;

    public CoachingController(GameRepository gameRepository, CoachingService coachingService) {
        this.gameRepository = gameRepository;
        this.coachingService = coachingService;
    }

    @GetMapping("/{id}/coaching")
    public ResponseEntity<CoachingResponse> getCoaching(@PathVariable Long id) {
        if (!gameRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        String summary = coachingService.generateCoachingSummary(id);
        return ResponseEntity.ok(new CoachingResponse(summary));
    }
}