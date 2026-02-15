package com.ghostauthor.knowledge.service;

import com.ghostauthor.knowledge.dto.DocumentResponse;

import java.util.List;

public interface SearchService {
    void indexDocument(Long id, String slug, String title, String summary, String content);
    void removeDocument(Long id);
    List<DocumentResponse> search(String keyword);
}
