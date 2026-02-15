package com.ghostauthor.knowledge.dto;

import jakarta.validation.constraints.Size;

public record DocumentMoveRequest(
        @Size(max = 128) String parentSlug
) {
}
