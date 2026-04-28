package com.cableops.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned from POST /api/auth/login and GET /api/auth/me.
 *
 * NOTE: No "token" field — the JWT travels only in the HttpOnly cookie,
 *       never in the response body.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String userId;
    private String userName;
    private String name;
    private String role;       // "ADMIN" | "Manager" | "Field Engineer"
}