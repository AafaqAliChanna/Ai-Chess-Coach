package com.chesscoach.backend.game;

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
    private Game game;

    @Column(name = "ply_number", nullable = false)
    private int plyNumber; // half-move count: 1 = White's 1st move, 2 = Black's 1st, etc.

    @Column(nullable = false)
    private String san; // Standard Algebraic Notation, e.g. "Nf3"

    @Column(name = "fen_after", nullable = false, columnDefinition = "TEXT")
    private String fenAfter; // full board state after this move

    public Move(Game game, int plyNumber, String san, String fenAfter) {
        this.game = game;
        this.plyNumber = plyNumber;
        this.san = san;
        this.fenAfter = fenAfter;
    }
}
