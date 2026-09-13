package com.chesscoach.backend.integrations.chesscom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ChessComModels {

    private ChessComModels() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChessComArchiveResponse(List<ChessComGame> games) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChessComGame(
            String url,
            String pgn,
            @JsonProperty("time_class") String timeClass,
            ChessComPlayer white,
            ChessComPlayer black
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChessComPlayer(String username, String result) {}
}