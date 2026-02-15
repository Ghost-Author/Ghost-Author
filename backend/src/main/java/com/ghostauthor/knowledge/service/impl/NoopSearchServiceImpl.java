package com.ghostauthor.knowledge.service.impl;

import com.ghostauthor.knowledge.dto.DocumentResponse;
import com.ghostauthor.knowledge.service.SearchService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "knowledge.search.enabled", havingValue = "false", matchIfMissing = true)
public class NoopSearchServiceImpl implements SearchService {

    @Override
    public void indexDocument(Long id, String slug, String title, String summary, String content) {
        // Search is disabled, so indexing is intentionally a no-op.
    }

    @Override
    public void removeDocument(Long id) {
        // Search is disabled, so deleting index entries is intentionally a no-op.
    }

    @Override
    public List<DocumentResponse> search(String keyword) {
        return List.of();
    }
}
