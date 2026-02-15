package com.ghostauthor.knowledge.service;

public interface FileStorageService {
    String writeMarkdown(String slug, String content);
    String readMarkdown(String path);
    void deleteMarkdown(String path);
}
