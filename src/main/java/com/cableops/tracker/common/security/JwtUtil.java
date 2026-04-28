package com.cableops.tracker.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    /**
     * PRODUCTION: set this in application.properties or as an env variable.
     *   app.jwt.secret=<random 64-char string>
     *
     * Never hard-code the secret in source code.
     */
    @Value("${app.jwt.secret:fiblink-crm-super-secret-key-2024-change-me!!}")
    private String secret;

    private static final long EXPIRY_MS = 8 * 60 * 60 * 1000L; // 8 hours

    private Key key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /** Generate a signed JWT containing userId, userName, and role. */
    public String generate(String userId, String userName, String role) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("userName", userName)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Parse and validate a token; throws JwtException on failure. */
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}