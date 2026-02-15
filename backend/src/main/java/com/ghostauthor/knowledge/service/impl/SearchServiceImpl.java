package com.ghostauthor.knowledge.service.impl;

import com.ghostauthor.knowledge.dto.DocumentResponse;
import com.ghostauthor.knowledge.entity.SearchDocument;
import com.ghostauthor.knowledge.repository.SearchDocumentRepository;
import com.ghostauthor.knowledge.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

    private final SearchDocumentRepository searchDocumentRepository;
    private final boolean searchEnabled;

    public SearchServiceImpl(SearchDocumentRepository searchDocumentRepository,
                             @Value("${knowledge.search.enabled:true}") boolean searchEnabled) {
        this.searchDocumentRepository = searchDocumentRepository;
        this.searchEnabled = searchEnabled;
    }

    @Override
    public void indexDocument(Long id, String slug, String title, String summary, String content) {
        if (!searchEnabled) {
            return;
        }
        try {
            searchDocumentRepository.save(new SearchDocument(id.toString(), slug, title, summary, content));
        } catch (Exception e) {
            logger.warn("Failed to index document {}: {}", slug, e.getMessage());
        }
    }

    @Override
    public void removeDocument(Long id) {
        if (!searchEnabled) {
            return;
        }
        try {
            searchDocumentRepository.deleteById(id.toString());
        } catch (Exception e) {
            logger.warn("Failed to remove indexed document {}: {}", id, e.getMessage());
        }
    }

    @Override
    public List<DocumentResponse> search(String keyword) {
        if (!searchEnabled) {
            return List.of();
        }

        List<SearchDocument> docs = searchDocumentRepository
                .findByTitleContainingOrSummaryContainingOrContentContaining(keyword, keyword, keyword);

        return docs.stream()
                .map(d -> new DocumentResponse(
                        Long.parseLong(d.getId()),
                        d.getSlug(),
                        d.getTitle(),
                        d.getSummary(),
                        d.getContent(),
                        null,
                        List.of(),
                        null,
                        List.of(),
                        List.of(),
                        null,
                        null,
                        false,
                        0,
                        false,
                        null,
                        null,
                        null
                ))
                .toList();
    }
}
