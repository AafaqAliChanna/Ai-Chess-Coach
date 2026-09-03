package com.chesscoach.backend.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "moves")
@Getter
@Setter
@NoArgsConstructor
public class Move {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    @JsonIgnore
    private Game game;

    @Column(name = "ply_number", nullable = false)
    private int plyNumber;

    @Column(nullable = false)
    private String san;

    @Column(name = "fen_after", nullable = false, columnDefinition = "TEXT")
    private String fenAfter;

    public Move(Game game, int plyNumber, String san, String fenAfter) {
        this.game = game;
        this.plyNumber = plyNumber;
        this.san = san;
        this.fenAfter = fenAfter;
    }
}