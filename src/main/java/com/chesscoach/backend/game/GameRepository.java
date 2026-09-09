package com.chesscoach.backend.game;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByWhitePlayerIgnoreCaseOrBlackPlayerIgnoreCase(String whitePlayer, String blackPlayer);
}
