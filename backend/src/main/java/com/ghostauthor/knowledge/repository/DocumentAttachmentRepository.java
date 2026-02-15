package com.ghostauthor.knowledge.repository;

import com.ghostauthor.knowledge.entity.DocumentAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentAttachmentRepository extends JpaRepository<DocumentAttachmentEntity, Long> {
    List<DocumentAttachmentEntity> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
    void deleteByDocumentId(Long documentId);
}
