package com.chesscoach.backend.coach;

public record OllamaGenerateRequest(String model, String prompt, boolean stream) {}