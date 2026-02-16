package com.ghostauthor.knowledge.dto;

public record AuthLoginResponse(
        String token,
        String username,
        String role,
        boolean mustChangePassword,
        long expiresAt
) {
}
