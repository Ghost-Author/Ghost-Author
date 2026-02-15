package com.ghostauthor.knowledge.repository;

import com.ghostauthor.knowledge.entity.DocumentVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersionEntity, Long> {
    List<DocumentVersionEntity> findByDocumentIdOrderByVersionNoDesc(Long documentId);
    Optional<DocumentVersionEntity> findTopByDocumentIdOrderByVersionNoDesc(Long documentId);
    Optional<DocumentVersionEntity> findByDocumentIdAndVersionNo(Long documentId, Integer versionNo);
}
