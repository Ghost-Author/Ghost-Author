package com.ghostauthor.knowledge.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String content,
        String parentSlug,
        List<String> labels,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
