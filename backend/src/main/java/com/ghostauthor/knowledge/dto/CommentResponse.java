package com.ghostauthor.knowledge.dto;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        String author,
        String content,
        LocalDateTime createdAt
) {
}
