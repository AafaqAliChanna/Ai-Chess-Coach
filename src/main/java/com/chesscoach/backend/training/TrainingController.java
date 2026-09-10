package com.chesscoach.backend.training;

import com.chesscoach.backend.analysis.GamePhase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class TrainingController {

    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @GetMapping("/{name}/training")
    public List<TrainingExercise> getTraining(
            @PathVariable String name,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) GamePhase phase) {
        return trainingService.buildExercises(name, limit, phase);
    }
}