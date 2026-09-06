package com.chesscoach.backend.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Hardcoded to the Next.js dev server for now. This becomes an
    // environment-specific value (dev origin vs. production domain) once
    // we deploy — same pattern as the Stockfish path being local-only:
    // don't generalize until there's a second real environment to support.
    private static final String FRONTEND_DEV_ORIGIN = "http://localhost:3000";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(FRONTEND_DEV_ORIGIN)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // browsers cache the preflight OPTIONS response for 1hr,
                               // reducing redundant preflight round-trips during dev
    }
}