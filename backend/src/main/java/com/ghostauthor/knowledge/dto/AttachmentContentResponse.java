package com.ghostauthor.knowledge.dto;

public record AttachmentContentResponse(
        String fileName,
        String contentType,
        byte[] bytes
) {
}
