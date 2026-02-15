package com.ghostauthor.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
        @Size(max = 64) String author,
        @NotBlank @Size(max = 2000) String content
) {
}
