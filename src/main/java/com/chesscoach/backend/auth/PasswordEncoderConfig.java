package com.chesscoach.backend.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt, not plain hashing (SHA-256, MD5, etc.) — BCrypt is
        // deliberately slow and includes a per-password salt automatically,
        // which is what actually makes stolen password hashes resistant to
        // brute-force/rainbow-table attacks. This is the industry-standard
        // choice for password storage specifically, not a general-purpose
        // hash.
        return new BCryptPasswordEncoder();
    }
}