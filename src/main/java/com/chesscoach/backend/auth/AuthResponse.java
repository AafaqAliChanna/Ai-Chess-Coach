package com.chesscoach.backend.auth;

public record AuthResponse(String token, Long userId, String email, String displayName) {}