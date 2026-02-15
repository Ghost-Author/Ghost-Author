package com.ghostauthor.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthUserUpsertRequest(
        @NotBlank @Size(max = 64) String username,
        @Size(max = 128) String password,
        @NotBlank @Size(max = 16) String role
) {
}
