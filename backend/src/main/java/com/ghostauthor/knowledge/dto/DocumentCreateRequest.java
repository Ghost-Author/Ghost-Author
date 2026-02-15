package com.ghostauthor.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.ghostauthor.knowledge.entity.DocumentStatus;

import java.util.List;

public record DocumentCreateRequest(
        @NotBlank @Size(max = 128) String slug,
        @NotBlank @Size(max = 256) String title,
        @NotBlank @Size(max = 512) String summary,
        @NotBlank String content,
        @Size(max = 128) String parentSlug,
        List<@Size(max = 32) String> labels,
        DocumentStatus status
) {
}
