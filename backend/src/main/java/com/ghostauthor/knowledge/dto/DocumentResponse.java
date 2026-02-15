package com.ghostauthor.knowledge.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.ghostauthor.knowledge.entity.DocumentStatus;

public record DocumentResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String content,
        String parentSlug,
        List<String> labels,
        DocumentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
