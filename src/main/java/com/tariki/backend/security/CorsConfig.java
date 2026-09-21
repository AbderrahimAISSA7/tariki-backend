package com.tariki.backend.security;

import java.util.List;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins:}") String extraOrigins) {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://localhost:*",
                "https://127.0.0.1:*"
        ));
        configuration.setAllowedOrigins(Arrays.stream(extraOrigins.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).map(origin -> {
                    java.net.URI uri = java.net.URI.create(origin);
                    if (!List.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null
                            || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                            || (uri.getPath() != null && !uri.getPath().isEmpty())) {
                        throw new IllegalArgumentException("CORS_ALLOWED_ORIGINS exige des origines HTTP(S) exactes, sans chemin ni joker");
                    }
                    return origin;
                }).toList());

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Accept", "Origin")
        );

        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
