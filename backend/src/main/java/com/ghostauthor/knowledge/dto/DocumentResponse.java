package com.ghostauthor.knowledge.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.ghostauthor.knowledge.entity.DocumentStatus;
import com.ghostauthor.knowledge.entity.DocumentVisibility;

public record DocumentResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String content,
        String parentSlug,
        List<String> labels,
        String owner,
        List<String> editors,
        List<String> viewers,
        DocumentStatus status,
        DocumentVisibility visibility,
        Boolean locked,
        Integer sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
