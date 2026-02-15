package com.ghostauthor.knowledge.dto;

import java.time.LocalDateTime;

public record AttachmentResponse(
        Long id,
        String fileName,
        String contentType,
        Long fileSize,
        String contentUrl,
        LocalDateTime createdAt
) {
}
