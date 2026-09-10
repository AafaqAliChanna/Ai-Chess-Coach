package com.chesscoach.backend.coach;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

public class GeminiModels {

    private GeminiModels() {}

    // ----- Request -----
    public record GenerateRequest(List<Content> contents) {
        public static GenerateRequest ofPrompt(String prompt) {
            return new GenerateRequest(List.of(new Content(List.of(new Part(prompt)))));
        }
    }
    public record Content(List<Part> parts) {}
    public record Part(String text) {}

    // ----- Response -----
    // ignoreUnknown=true throughout: Gemini's actual response includes many
    // more fields (safety ratings, finish reason, usage metadata...) we
    // don't need — same reasoning as OllamaGenerateResponse in Phase 6.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GenerateResponse(List<Candidate> candidates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content) {}
}