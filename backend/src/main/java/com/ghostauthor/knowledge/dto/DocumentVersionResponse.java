package com.ghostauthor.knowledge.dto;

import java.time.LocalDateTime;

public record DocumentVersionResponse(
        Long id,
        Integer versionNo,
        String title,
        String summary,
        String content,
        LocalDateTime createdAt
) {
}
