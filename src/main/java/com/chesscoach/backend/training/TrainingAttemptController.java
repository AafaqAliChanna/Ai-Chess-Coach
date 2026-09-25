package com.chesscoach.backend.training;

import com.chesscoach.backend.auth.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training")
public class TrainingAttemptController {

    private final TrainingAttemptService trainingAttemptService;
    private final CurrentUserProvider currentUserProvider;

    public TrainingAttemptController(TrainingAttemptService trainingAttemptService,
                                      CurrentUserProvider currentUserProvider) {
        this.trainingAttemptService = trainingAttemptService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/attempts")
    public ResponseEntity<TrainingAttemptResponse> recordAttempt(@Valid @RequestBody TrainingAttemptRequest request) {
        Long userId = currentUserProvider.requireCurrentUserId();
        TrainingAttemptResponse response = trainingAttemptService.recordAttempt(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}