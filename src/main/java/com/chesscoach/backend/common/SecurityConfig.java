package com.chesscoach.backend.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * PART 1 SCOPE: Spring Security is wired in (stateless, CSRF off — correct
 * baseline for a token-based JSON API) but every existing endpoint stays
 * fully open (permitAll). /api/auth/** issues real tokens starting now.
 * Actually REQUIRING a valid token on game/analysis/training endpoints,
 * and wiring Game.userId to the authenticated user, is Part 2 — a
 * deliberate, separately-tested next step, not an oversight.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}