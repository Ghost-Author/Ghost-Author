package com.ghostauthor.knowledge.repository;

import com.ghostauthor.knowledge.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    Optional<DocumentEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<DocumentEntity> findAllByOrderByParentSlugAscSortOrderAscCreatedAtAsc();
    List<DocumentEntity> findByParentSlugOrderBySortOrderAscCreatedAtAsc(String parentSlug);
    List<DocumentEntity> findByParentSlugIsNullOrderBySortOrderAscCreatedAtAsc();
}
