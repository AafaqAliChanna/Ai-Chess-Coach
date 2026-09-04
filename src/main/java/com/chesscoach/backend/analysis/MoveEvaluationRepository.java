package com.chesscoach.backend.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MoveEvaluationRepository extends JpaRepository<MoveEvaluation, Long> {
}