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

    private String title; // nullable — frontend falls back to "White vs Black" when absent

    @Column(name = "white_player")
    private String whitePlayer;

    @Column(name = "black_player")
    private String blackPlayer;

    private String result;

    // Nullable, same reasoning as title: bullet/blitz/rapid/daily, supplied
    // explicitly by the caller rather than parsed here. Manual uploads have
    // no PGN headers left by the time they reach this entity (stripped in
    // PgnParsingService before this point), so there's nothing to parse from
    // even if we wanted to. Chess.com imports populate this from the
    // Chess.com API's own time_class field instead — see
    // ChessComImportService, which already has it available per-game.
    @Column(name = "time_control")
    private String timeControl;

    // Nullable on purpose, and no relationship/User entity yet — accounts don't
    // exist until Phase 9. Adding the raw column now avoids a backfill migration
    // once real user data exists; wiring it to an actual User FK is future work,
    // not a schema change.
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "source_url", unique = true)
    private String sourceUrl;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt = Instant.now();
}