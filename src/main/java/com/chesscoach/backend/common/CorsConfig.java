package com.chesscoach.backend.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    // TODO(Phase 11 - deployment): add the real production frontend domain
    // here alongside localhost:3000.
    private static final String FRONTEND_DEV_ORIGIN = "http://localhost:3000";

    // Exposed as a CorsConfigurationSource bean specifically so Spring
    // Security's own .cors() DSL can pick it up directly, rather than
    // relying on WebMvcConfigurer — which runs too late in the pipeline
    // now that Security is enforcing auth on preflight-triggering methods.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(FRONTEND_DEV_ORIGIN));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*")); // includes Authorization
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}