package com.chesscoach.backend.game;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MoveRepository extends JpaRepository<Move, Long> {
    List<Move> findByGameIdOrderByPlyNumberAsc(Long gameId);
}