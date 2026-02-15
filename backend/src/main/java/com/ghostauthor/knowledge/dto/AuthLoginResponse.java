package com.ghostauthor.knowledge.dto;

public record AuthLoginResponse(
        String token,
        String username,
        String role,
        long expiresAt
) {
}
