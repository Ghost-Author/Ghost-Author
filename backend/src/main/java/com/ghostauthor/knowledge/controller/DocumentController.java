package com.ghostauthor.knowledge.controller;

import com.ghostauthor.knowledge.dto.DocumentCreateRequest;
import com.ghostauthor.knowledge.dto.DocumentResponse;
import com.ghostauthor.knowledge.dto.DocumentUpdateRequest;
import com.ghostauthor.knowledge.dto.DocumentVersionDiffResponse;
import com.ghostauthor.knowledge.dto.DocumentVersionResponse;
import com.ghostauthor.knowledge.dto.DocumentMoveRequest;
import com.ghostauthor.knowledge.dto.DocumentReorderRequest;
import com.ghostauthor.knowledge.dto.CommentCreateRequest;
import com.ghostauthor.knowledge.dto.CommentResponse;
import com.ghostauthor.knowledge.dto.AttachmentResponse;
import com.ghostauthor.knowledge.dto.AttachmentContentResponse;
import com.ghostauthor.knowledge.service.DocumentService;
import com.ghostauthor.knowledge.service.SearchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final SearchService searchService;

    public DocumentController(DocumentService documentService, SearchService searchService) {
        this.documentService = documentService;
        this.searchService = searchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse create(@Valid @RequestBody DocumentCreateRequest request) {
        return documentService.create(request);
    }

    @PutMapping("/{slug}")
    public DocumentResponse update(@PathVariable String slug, @Valid @RequestBody DocumentUpdateRequest request) {
        return documentService.update(slug, request);
    }

    @GetMapping("/{slug}")
    public DocumentResponse getBySlug(@PathVariable String slug) {
        return documentService.getBySlug(slug);
    }

    @GetMapping
    public List<DocumentResponse> list() {
        return documentService.list();
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String slug) {
        documentService.delete(slug);
    }

    @GetMapping("/{slug}/versions")
    public List<DocumentVersionResponse> listVersions(@PathVariable String slug) {
        return documentService.listVersions(slug);
    }

    @PostMapping("/{slug}/versions/{versionNo}/restore")
    public DocumentResponse restoreVersion(@PathVariable String slug, @PathVariable Integer versionNo) {
        return documentService.restoreVersion(slug, versionNo);
    }

    @PatchMapping("/{slug}/move")
    public DocumentResponse move(@PathVariable String slug, @Valid @RequestBody DocumentMoveRequest request) {
        return documentService.move(slug, request);
    }

    @PatchMapping("/{slug}/reorder")
    public DocumentResponse reorder(@PathVariable String slug, @Valid @RequestBody DocumentReorderRequest request) {
        return documentService.reorder(slug, request);
    }

    @GetMapping("/{slug}/versions/diff")
    public DocumentVersionDiffResponse diffVersions(@PathVariable String slug,
                                                    @RequestParam("from") Integer fromVersion,
                                                    @RequestParam("to") Integer toVersion) {
        return documentService.diffVersions(slug, fromVersion, toVersion);
    }

    @GetMapping("/{slug}/comments")
    public List<CommentResponse> listComments(@PathVariable String slug) {
        return documentService.listComments(slug);
    }

    @PostMapping("/{slug}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable String slug, @Valid @RequestBody CommentCreateRequest request) {
        return documentService.addComment(slug, request);
    }

    @DeleteMapping("/{slug}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable String slug, @PathVariable Long commentId) {
        documentService.deleteComment(slug, commentId);
    }

    @GetMapping("/{slug}/attachments")
    public List<AttachmentResponse> listAttachments(@PathVariable String slug) {
        return documentService.listAttachments(slug);
    }

    @PostMapping("/{slug}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse uploadAttachment(@PathVariable String slug,
                                               @RequestParam("file") MultipartFile file) {
        return documentService.uploadAttachment(slug, file);
    }

    @GetMapping("/{slug}/attachments/{attachmentId}/content")
    public ResponseEntity<byte[]> getAttachmentContent(@PathVariable String slug, @PathVariable Long attachmentId) {
        AttachmentContentResponse content = documentService.getAttachmentContent(slug, attachmentId);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (content.contentType() != null && !content.contentType().isBlank()) {
            mediaType = MediaType.parseMediaType(content.contentType());
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + content.fileName() + "\"")
                .contentType(mediaType)
                .body(content.bytes());
    }

    @DeleteMapping("/{slug}/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttachment(@PathVariable String slug, @PathVariable Long attachmentId) {
        documentService.deleteAttachment(slug, attachmentId);
    }

    @GetMapping("/search")
    public List<DocumentResponse> search(@RequestParam("q") String keyword) {
        return searchService.search(keyword);
    }
}
