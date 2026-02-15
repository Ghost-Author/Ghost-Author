package com.ghostauthor.knowledge.repository;

import com.ghostauthor.knowledge.entity.SearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface SearchDocumentRepository extends ElasticsearchRepository<SearchDocument, String> {
    List<SearchDocument> findByTitleContainingOrSummaryContainingOrContentContaining(String title, String summary, String content);
}
