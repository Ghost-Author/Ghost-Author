package com.ghostauthor.knowledge.repository;

import com.ghostauthor.knowledge.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    Optional<DocumentEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
