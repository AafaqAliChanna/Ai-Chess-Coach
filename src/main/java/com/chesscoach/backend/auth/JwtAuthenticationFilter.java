package com.chesscoach.backend.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs once per request. If a valid "Authorization: Bearer <token>" header
 * is present, populates Spring Security's context with the token's userId
 * as the principal. If the header is missing or the token is invalid, this
 * filter does nothing and lets the request continue unauthenticated —
 * SecurityConfig's authorization rules decide what happens next (allowed
 * through for open GETs, rejected with 401 for protected endpoints).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            try {
                Long userId = jwtService.validateAndGetUserId(header.substring(7));
                var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (AuthException ignored) {
                // Invalid/expired token — leave context empty rather than
                // failing the request here. This filter's only job is
                // "parse a token if one exists," not "decide access."
            }
        }

        filterChain.doFilter(request, response);
    }
}