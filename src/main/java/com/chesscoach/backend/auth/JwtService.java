package com.chesscoach.backend.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey signingKey;
    private static final Duration TOKEN_VALIDITY = Duration.ofDays(7);

    public JwtService(AuthProperties properties) {
        // HMAC-SHA256 requires a key of at least 256 bits (32 bytes). The
        // configured secret is used as raw key material directly — validated
        // at startup (see AuthProperties) rather than failing confusingly
        // the first time a token is actually issued.
        this.signingKey = Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes());
    }

    public String generateToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(TOKEN_VALIDITY)))
                .signWith(signingKey)
                .compact();
    }

    /** Returns the userId if the token is valid; throws AuthException otherwise. */
    public Long validateAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException("Invalid or expired token");
        }
    }
}