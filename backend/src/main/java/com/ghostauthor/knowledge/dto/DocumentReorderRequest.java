package com.ghostauthor.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DocumentReorderRequest(
        @NotBlank
        @Pattern(regexp = "UP|DOWN", message = "Direction must be UP or DOWN")
        String direction
) {
}
