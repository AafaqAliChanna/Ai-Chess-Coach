package com.chesscoach.backend.coach;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Ollama's actual response includes many more fields (done, context, timing
// stats, token counts...) — we only care about "response". @JsonIgnoreProperties
// means Jackson silently drops everything else instead of throwing on unknown
// fields, so a future Ollama version adding new fields won't break deserialization.
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaGenerateResponse(String response) {}