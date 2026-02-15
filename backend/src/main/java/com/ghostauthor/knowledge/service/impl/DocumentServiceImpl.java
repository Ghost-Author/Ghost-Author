package com.ghostauthor.knowledge.service.impl;

import com.ghostauthor.knowledge.dto.DocumentCreateRequest;
import com.ghostauthor.knowledge.dto.DocumentResponse;
import com.ghostauthor.knowledge.dto.DocumentUpdateRequest;
import com.ghostauthor.knowledge.dto.DocumentVersionDiffResponse;
import com.ghostauthor.knowledge.dto.DocumentVersionResponse;
import com.ghostauthor.knowledge.dto.DocumentMoveRequest;
import com.ghostauthor.knowledge.dto.CommentCreateRequest;
import com.ghostauthor.knowledge.dto.CommentResponse;
import com.ghostauthor.knowledge.dto.AttachmentResponse;
import com.ghostauthor.knowledge.dto.AttachmentContentResponse;
import com.ghostauthor.knowledge.entity.DocumentAttachmentEntity;
import com.ghostauthor.knowledge.entity.DocumentCommentEntity;
import com.ghostauthor.knowledge.entity.DocumentEntity;
import com.ghostauthor.knowledge.entity.DocumentStatus;
import com.ghostauthor.knowledge.entity.DocumentVisibility;
import com.ghostauthor.knowledge.entity.DocumentVersionEntity;
import com.ghostauthor.knowledge.repository.DocumentAttachmentRepository;
import com.ghostauthor.knowledge.repository.DocumentCommentRepository;
import com.ghostauthor.knowledge.repository.DocumentRepository;
import com.ghostauthor.knowledge.repository.DocumentVersionRepository;
import com.ghostauthor.knowledge.service.DocumentService;
import com.ghostauthor.knowledge.service.FileStorageService;
import com.ghostauthor.knowledge.service.SearchService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentAttachmentRepository documentAttachmentRepository;
    private final DocumentCommentRepository documentCommentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final FileStorageService fileStorageService;
    private final SearchService searchService;

    public DocumentServiceImpl(DocumentRepository documentRepository,
                               DocumentAttachmentRepository documentAttachmentRepository,
                               DocumentCommentRepository documentCommentRepository,
                               DocumentVersionRepository documentVersionRepository,
                               FileStorageService fileStorageService,
                               SearchService searchService) {
        this.documentRepository = documentRepository;
        this.documentAttachmentRepository = documentAttachmentRepository;
        this.documentCommentRepository = documentCommentRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.fileStorageService = fileStorageService;
        this.searchService = searchService;
    }

    @Override
    @Transactional
    public DocumentResponse create(DocumentCreateRequest request) {
        if (documentRepository.existsBySlug(request.slug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists");
        }

        String filePath = fileStorageService.writeMarkdown(request.slug(), request.content());

        DocumentEntity entity = new DocumentEntity();
        entity.setSlug(request.slug());
        entity.setTitle(request.title());
        entity.setSummary(request.summary());
        entity.setParentSlug(normalizeParentSlug(request.slug(), request.parentSlug()));
        entity.setLabels(joinLabels(request.labels()));
        entity.setStatus(request.status() == null ? DocumentStatus.DRAFT : request.status());
        entity.setVisibility(request.visibility() == null ? DocumentVisibility.SPACE : request.visibility());
        entity.setLocked(Boolean.TRUE.equals(request.locked()));
        entity.setFilePath(filePath);

        DocumentEntity saved = documentRepository.save(entity);
        saveVersion(saved.getId(), saved.getTitle(), saved.getSummary(), request.content());
        searchService.indexDocument(saved.getId(), saved.getSlug(), saved.getTitle(), saved.getSummary(), request.content());

        return toResponse(saved, request.content());
    }

    @Override
    @Transactional
    public DocumentResponse update(String slug, DocumentUpdateRequest request) {
        DocumentEntity entity = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        fileStorageService.writeMarkdown(slug, request.content());
        entity.setTitle(request.title());
        entity.setSummary(request.summary());
        entity.setParentSlug(normalizeParentSlug(slug, request.parentSlug()));
        entity.setLabels(joinLabels(request.labels()));
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        if (request.visibility() != null) {
            entity.setVisibility(request.visibility());
        }
        if (request.locked() != null) {
            entity.setLocked(request.locked());
        }

        DocumentEntity saved = documentRepository.save(entity);
        saveVersion(saved.getId(), saved.getTitle(), saved.getSummary(), request.content());
        searchService.indexDocument(saved.getId(), saved.getSlug(), saved.getTitle(), saved.getSummary(), request.content());

        return toResponse(saved, request.content());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getBySlug(String slug) {
        DocumentEntity entity = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        String content = fileStorageService.readMarkdown(entity.getFilePath());
        return toResponse(entity, content);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> list() {
        return documentRepository.findAll().stream()
                .map(entity -> toResponse(entity, null))
                .toList();
    }

    @Override
    @Transactional
    public void delete(String slug) {
        DocumentEntity entity = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        fileStorageService.deleteMarkdown(entity.getFilePath());
        documentAttachmentRepository.findByDocumentIdOrderByCreatedAtDesc(entity.getId())
                .forEach(attachment -> fileStorageService.deleteFile(attachment.getFilePath()));
        documentAttachmentRepository.deleteByDocumentId(entity.getId());
        documentCommentRepository.deleteByDocumentId(entity.getId());
        documentRepository.delete(entity);
        searchService.removeDocument(entity.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentVersionResponse> listVersions(String slug) {
        DocumentEntity entity = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        return documentVersionRepository.findByDocumentIdOrderByVersionNoDesc(entity.getId()).stream()
                .map(this::toVersionResponse)
                .toList();
    }

    @Override
    @Transactional
    public DocumentResponse restoreVersion(String slug, Integer versionNo) {
        DocumentEntity entity = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        DocumentVersionEntity version = documentVersionRepository.findByDocumentIdAndVersionNo(entity.getId(), versionNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));

        fileStorageService.writeMarkdown(slug, version.getContent());
        entity.setTitle(version.getTitle());
        entity.setSummary(version.getSummary());
        DocumentEntity saved = documentRepository.save(entity);

        // Restoring also creates a new latest version snapshot.
        saveVersion(saved.getId(), version.getTitle(), version.getSummary(), version.getContent());
        searchService.indexDocument(saved.getId(), saved.getSlug(), saved.getTitle(), saved.getSummary(), version.getContent());

        return toResponse(saved, version.getContent());
    }

    @Override
    @Transactional
    public DocumentResponse move(String slug, DocumentMoveRequest request) {
        DocumentEntity entity = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        entity.setParentSlug(normalizeParentSlug(slug, request.parentSlug()));
        DocumentEntity saved = documentRepository.save(entity);
        String content = fileStorageService.readMarkdown(saved.getFilePath());
        return toResponse(saved, content);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentVersionDiffResponse diffVersions(String slug, Integer fromVersion, Integer toVersion) {
        DocumentEntity entity = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        DocumentVersionEntity from = documentVersionRepository.findByDocumentIdAndVersionNo(entity.getId(), fromVersion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "From version not found"));
        DocumentVersionEntity to = documentVersionRepository.findByDocumentIdAndVersionNo(entity.getId(), toVersion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "To version not found"));

        List<String> sourceLines = Arrays.asList(from.getContent().split("\\R", -1));
        List<String> targetLines = Arrays.asList(to.getContent().split("\\R", -1));
        Patch<String> patch = DiffUtils.diff(sourceLines, targetLines);
        List<String> unified = UnifiedDiffUtils.generateUnifiedDiff(
                "v" + fromVersion,
                "v" + toVersion,
                sourceLines,
                patch,
                3
        );

        return new DocumentVersionDiffResponse(fromVersion, toVersion, String.join("\n", unified));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(String slug) {
        DocumentEntity entity = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        return documentCommentRepository.findByDocumentIdOrderByCreatedAtAsc(entity.getId()).stream()
                .map(this::toCommentResponse)
                .toList();
    }

    @Override
    @Transactional
    public CommentResponse addComment(String slug, CommentCreateRequest request) {
        DocumentEntity entity = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        DocumentCommentEntity comment = new DocumentCommentEntity();
        comment.setDocumentId(entity.getId());
        comment.setAuthor(StringUtils.hasText(request.author()) ? request.author().trim() : "Anonymous");
        comment.setContent(request.content().trim());
        return toCommentResponse(documentCommentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(String slug, Long commentId) {
        DocumentEntity entity = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        DocumentCommentEntity comment = documentCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (!comment.getDocumentId().equals(entity.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment does not belong to the document");
        }
        documentCommentRepository.delete(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> listAttachments(String slug) {
        DocumentEntity entity = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        return documentAttachmentRepository.findByDocumentIdOrderByCreatedAtDesc(entity.getId()).stream()
                .map(attachment -> toAttachmentResponse(slug, attachment))
                .toList();
    }

    @Override
    @Transactional
    public AttachmentResponse uploadAttachment(String slug, MultipartFile file) {
        DocumentEntity entity = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment file is empty");
        }

        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "file.bin";
        String path;
        try {
            path = fileStorageService.writeAttachment(slug, originalName, file.getBytes());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store attachment");
        }

        String storedName = path.substring(path.lastIndexOf(java.io.File.separator) + 1);
        DocumentAttachmentEntity attachment = new DocumentAttachmentEntity();
        attachment.setDocumentId(entity.getId());
        attachment.setOriginalFileName(originalName);
        attachment.setStoredFileName(storedName);
        attachment.setFilePath(path);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        DocumentAttachmentEntity saved = documentAttachmentRepository.save(attachment);
        return toAttachmentResponse(slug, saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentContentResponse getAttachmentContent(String slug, Long attachmentId) {
        DocumentEntity entity = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        DocumentAttachmentEntity attachment = documentAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        if (!attachment.getDocumentId().equals(entity.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment does not belong to the document");
        }
        byte[] bytes = fileStorageService.readBinary(attachment.getFilePath());
        return new AttachmentContentResponse(
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                bytes
        );
    }

    @Override
    @Transactional
    public void deleteAttachment(String slug, Long attachmentId) {
        DocumentEntity entity = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        DocumentAttachmentEntity attachment = documentAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        if (!attachment.getDocumentId().equals(entity.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment does not belong to the document");
        }
        fileStorageService.deleteFile(attachment.getFilePath());
        documentAttachmentRepository.delete(attachment);
    }

    private DocumentResponse toResponse(DocumentEntity entity, String content) {
        return new DocumentResponse(
                entity.getId(),
                entity.getSlug(),
                entity.getTitle(),
                entity.getSummary(),
                content,
                entity.getParentSlug(),
                splitLabels(entity.getLabels()),
                entity.getStatus() == null ? DocumentStatus.DRAFT : entity.getStatus(),
                entity.getVisibility() == null ? DocumentVisibility.SPACE : entity.getVisibility(),
                Boolean.TRUE.equals(entity.getLocked()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private void saveVersion(Long documentId, String title, String summary, String content) {
        int nextVersion = documentVersionRepository.findTopByDocumentIdOrderByVersionNoDesc(documentId)
                .map(v -> v.getVersionNo() + 1)
                .orElse(1);

        DocumentVersionEntity version = new DocumentVersionEntity();
        version.setDocumentId(documentId);
        version.setVersionNo(nextVersion);
        version.setTitle(title);
        version.setSummary(summary);
        version.setContent(content);
        documentVersionRepository.save(version);
    }

    private DocumentVersionResponse toVersionResponse(DocumentVersionEntity version) {
        return new DocumentVersionResponse(
                version.getId(),
                version.getVersionNo(),
                version.getTitle(),
                version.getSummary(),
                version.getContent(),
                version.getCreatedAt()
        );
    }

    private String normalizeParentSlug(String currentSlug, String parentSlug) {
        if (!StringUtils.hasText(parentSlug)) {
            return null;
        }
        String normalized = parentSlug.trim();
        if (normalized.equals(currentSlug)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent slug cannot be self");
        }
        DocumentEntity cursor = documentRepository.findBySlug(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent document does not exist"));

        // Prevent cyclic nesting: current page cannot be placed under its descendants.
        while (cursor != null && StringUtils.hasText(cursor.getParentSlug())) {
            if (currentSlug.equals(cursor.getSlug())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent creates cyclic hierarchy");
            }
            cursor = documentRepository.findBySlug(cursor.getParentSlug()).orElse(null);
        }
        if (cursor != null && currentSlug.equals(cursor.getSlug())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent creates cyclic hierarchy");
        }
        return normalized;
    }

    private String joinLabels(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return null;
        }
        return labels.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private List<String> splitLabels(String labelsCsv) {
        if (!StringUtils.hasText(labelsCsv)) {
            return List.of();
        }
        return Arrays.stream(labelsCsv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private CommentResponse toCommentResponse(DocumentCommentEntity comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthor(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }

    private AttachmentResponse toAttachmentResponse(String slug, DocumentAttachmentEntity attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getFileSize(),
                "/api/documents/" + slug + "/attachments/" + attachment.getId() + "/content",
                attachment.getCreatedAt()
        );
    }
}
