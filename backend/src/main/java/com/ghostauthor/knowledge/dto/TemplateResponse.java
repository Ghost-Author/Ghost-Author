package com.ghostauthor.knowledge.dto;

import java.time.LocalDateTime;

public record TemplateResponse(
        Long id,
        String name,
        String description,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
