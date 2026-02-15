package com.ghostauthor.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TemplateCreateRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 256) String description,
        @NotBlank String content
) {
}
