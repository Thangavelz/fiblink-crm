package com.cableops.tracker.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        String token = extractToken(req);

        if (token != null && jwtUtil.isValid(token)) {
            Claims claims = jwtUtil.parse(token);
            String role   = claims.get("role", String.class);
            String userId = claims.getSubject();

            var auth = new UsernamePasswordAuthenticationToken(
                    userId, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + normalise(role)))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(req, res);
    }

    /**
     * Token extraction priority:
     * 1. HttpOnly cookie "jwt"          — browser clients (production)
     * 2. Authorization: Bearer <token>  — API / mobile clients
     * 3. ?token=<token>                 — browser <img> / download links
     */
    private String extractToken(HttpServletRequest req) {

        // 1. HttpOnly cookie
        if (req.getCookies() != null) {
            String cookieToken = Arrays.stream(req.getCookies())
                    .filter(c -> "jwt".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
            if (cookieToken != null && !cookieToken.isBlank()) return cookieToken;
        }

        // 2. Authorization header
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        // 3. Query param (attachment downloads)
        String qp = req.getParameter("token");
        if (qp != null && !qp.isBlank()) return qp;

        return null;
    }

    private String normalise(String role) {
        if (role == null) return "UNKNOWN";
        return role.trim().toUpperCase().replace(" ", "_");
    }
}