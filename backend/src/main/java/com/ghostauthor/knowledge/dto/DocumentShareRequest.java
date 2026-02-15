package com.ghostauthor.knowledge.dto;

public record DocumentShareRequest(
        Boolean enabled,
        Boolean regenerate
) {
}
