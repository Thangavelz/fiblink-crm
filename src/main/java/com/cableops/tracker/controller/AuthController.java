package com.cableops.tracker.controller;

import com.cableops.tracker.common.security.JwtUtil;
import com.cableops.tracker.dto.LoginRequest;
import com.cableops.tracker.dto.LoginResponse;
import com.cableops.tracker.entity.User;
import com.cableops.tracker.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
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
     * Body: { "userName": "...", "password": "..." }
     *
     * - Sets an HttpOnly Secure SameSite=Strict cookie named "jwt"
     * - Returns user metadata (NO token in body)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest req,
            HttpServletResponse response) {

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

        // Set token as HttpOnly cookie — JS cannot read this
        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(false)          // set true in production (requires HTTPS)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofHours(8))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // Return user metadata only — token stays in cookie, never in body
        LoginResponse body = new LoginResponse(
                user.getId(),
                user.getUserName(),
                user.getName(),
                user.getType()
        );

        return ResponseEntity.ok(body);
    }

    /**
     * POST /api/auth/logout
     * Clears the jwt cookie by setting maxAge=0
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(false)          // match the login setting
                .sameSite("Strict")
                .path("/")
                .maxAge(0)              // expire immediately
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /**
     * GET /api/auth/me
     * Returns current user info from the JWT cookie — used on page reload
     * to restore session without re-login.
     */
    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(jakarta.servlet.http.HttpServletRequest request) {
        String token = extractCookieToken(request);

        if (token == null || !jwtUtil.isValid(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        var claims = jwtUtil.parse(token);
        String userId   = claims.getSubject();
        String userName = claims.get("userName", String.class);
        String role     = claims.get("role",     String.class);

        User user = userRepo.findByUserName(userName)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        return ResponseEntity.ok(new LoginResponse(userId, userName, user.getName(), role));
    }

    private String extractCookieToken(jakarta.servlet.http.HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (var c : request.getCookies()) {
            if ("jwt".equals(c.getName())) return c.getValue();
        }
        return null;
    }
}