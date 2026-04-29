package com.cableops.tracker.controller;

import com.cableops.tracker.common.security.JwtUtil;
import com.cableops.tracker.dto.LoginRequest;
import com.cableops.tracker.dto.LoginResponse;
import com.cableops.tracker.entity.User;
import com.cableops.tracker.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository  userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil         jwtUtil;

    /**
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest req,
            jakarta.servlet.http.HttpServletResponse response) {

        User user = userRepo.findByUserName(req.getUserName())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is inactive");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtUtil.generate(user.getId(), user.getUserName(), user.getType());

        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofHours(8))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new LoginResponse(
                user.getId(), user.getUserName(), user.getName(), user.getType()
        ));
    }

    /**
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            jakarta.servlet.http.HttpServletResponse response) {

        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /**
     * GET /api/auth/me
     *
     * Validates the JWT cookie AND checks DB for:
     *  - User still exists
     *  - User is still active (isActive = true)
     *  - Returns fresh name/role from DB (picks up admin changes in real time)
     *
     * Returns 401 if any check fails → frontend logs out immediately.
     */
    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(
            jakarta.servlet.http.HttpServletRequest request) {

        String token = extractCookieToken(request);

        // 1. No cookie or invalid JWT
        if (token == null || !jwtUtil.isValid(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        Claims claims   = jwtUtil.parse(token);
        String userName = claims.get("userName", String.class);

        // 2. Look up user in DB — catches deleted users
        User user = userRepo.findByUserName(userName)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // 3. Check isActive — catches deactivated users immediately
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is inactive");
        }

        // Return fresh data from DB (name or role may have changed)
        return ResponseEntity.ok(new LoginResponse(
                user.getId(),
                user.getUserName(),
                user.getName(),    // fresh from DB
                user.getType()     // fresh from DB
        ));
    }

    private String extractCookieToken(jakarta.servlet.http.HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (var c : request.getCookies()) {
            if ("jwt".equals(c.getName())) return c.getValue();
        }
        return null;
    }
}