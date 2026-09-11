package com.chesscoach.backend.auth;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private String jwtSecret;

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }

    @PostConstruct
    public void validate() {
        // Fail loudly at startup, not on the first real login attempt —
        // same "fail fast, fail clear" principle as StockfishEngine's
        // bounded timeouts back in Phase 3.
        if (jwtSecret == null || jwtSecret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "auth.jwt-secret must be set and at least 32 bytes (256 bits) long for HS256 signing. "
                            + "Set it in application-local.yml.");
        }
    }
}