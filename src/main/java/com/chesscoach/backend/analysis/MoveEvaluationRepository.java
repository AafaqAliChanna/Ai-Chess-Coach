package com.chesscoach.backend.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MoveEvaluationRepository extends JpaRepository<MoveEvaluation, Long> {
    List<MoveEvaluation> findByMove_GameIdOrderByMove_PlyNumberAsc(Long gameId);
}