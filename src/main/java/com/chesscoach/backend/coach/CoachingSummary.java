package com.chesscoach.backend.coach;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CoachingSummary(
        List<String> strengths,
        List<String> weaknesses,
        List<KeyMoment> keyMoments,
        String recommendation
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KeyMoment(int plyNumber, String comment) {}
}