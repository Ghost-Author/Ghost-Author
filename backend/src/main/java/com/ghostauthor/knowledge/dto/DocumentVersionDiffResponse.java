package com.ghostauthor.knowledge.dto;

public record DocumentVersionDiffResponse(
        Integer fromVersion,
        Integer toVersion,
        String diff
) {
}
