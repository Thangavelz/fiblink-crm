package com.cableops.tracker.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * PRODUCTION: set allowed origins in application.properties
     *   app.cors.allowed-origins=https://yourdomain.com
     *
     * For local dev the default covers http://localhost:*
     */
    @Value("${app.cors.allowed-origins:http://localhost:*}")
    private String allowedOriginPattern;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Pattern required when allowCredentials = true
        config.addAllowedOriginPattern(allowedOriginPattern);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");           // includes OPTIONS
        config.setAllowCredentials(true);       // required for cookies

        // Headers the frontend needs to read
        config.setExposedHeaders(List.of("X-Total-Count", "Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}