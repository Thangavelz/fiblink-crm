package com.cableops.tracker.common.config;

import com.cableops.tracker.common.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Role matrix:
     *
     *  ADMIN          → full access
     *  MANAGER        → everything an engineer can do + reassign tasks
     *  FIELD_ENGINEER → GET any resource; limited PUT (enforced in service)
     *
     * Public endpoints:
     *  POST /api/auth/login    — obtain cookie
     *  POST /api/auth/logout   — clear cookie
     *  GET  /api/auth/me       — restore session on reload (cookie required)
     */
    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        http
            // Plug our CORS config at the top of the Spring Security filter chain
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 1. All preflight OPTIONS — must be first
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 2. Auth endpoints — public
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/auth/me").permitAll()
                // 3. Attachment / file endpoints
                .requestMatchers("/api/v1/**").authenticated()
                // 4. Everything else requires a valid JWT cookie
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}