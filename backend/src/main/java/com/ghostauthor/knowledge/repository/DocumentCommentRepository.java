package com.ghostauthor.knowledge.repository;

import com.ghostauthor.knowledge.entity.DocumentCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentCommentRepository extends JpaRepository<DocumentCommentEntity, Long> {
    List<DocumentCommentEntity> findByDocumentIdOrderByCreatedAtAsc(Long documentId);
    void deleteByDocumentId(Long documentId);
}
