package com.chesscoach.backend.game;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String pgn;

    @Column(name = "white_player")
    private String whitePlayer;

    @Column(name = "black_player")
    private String blackPlayer;

    private String result;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt = Instant.now();
}