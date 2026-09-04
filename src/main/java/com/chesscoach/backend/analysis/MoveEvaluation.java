package com.chesscoach.backend.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.chesscoach.backend.game.Move;
import jakarta.persistence.*;

@Entity
@Table(name = "move_evaluations")
public class MoveEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "move_id", nullable = false, unique = true)
    @JsonIgnore
    private Move move;

    @Column(name = "best_move_uci", nullable = false)
    private String bestMoveUci;

    private Integer scoreCentipawns; // null when mateInMoves is set

    private Integer mateInMoves; // null when scoreCentipawns is set

    protected MoveEvaluation() {
        // required by JPA
    }

    public MoveEvaluation(Move move, String bestMoveUci, Integer scoreCentipawns, Integer mateInMoves) {
        this.move = move;
        this.bestMoveUci = bestMoveUci;
        this.scoreCentipawns = scoreCentipawns;
        this.mateInMoves = mateInMoves;
    }

    public Long getId() { return id; }
    public Move getMove() { return move; }
    public String getBestMoveUci() { return bestMoveUci; }
    public Integer getScoreCentipawns() { return scoreCentipawns; }
    public Integer getMateInMoves() { return mateInMoves; }
}