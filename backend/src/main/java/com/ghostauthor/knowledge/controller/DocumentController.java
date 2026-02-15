package com.ghostauthor.knowledge.controller;

import com.ghostauthor.knowledge.dto.DocumentCreateRequest;
import com.ghostauthor.knowledge.dto.DocumentResponse;
import com.ghostauthor.knowledge.dto.DocumentUpdateRequest;
import com.ghostauthor.knowledge.dto.DocumentVersionDiffResponse;
import com.ghostauthor.knowledge.dto.DocumentVersionResponse;
import com.ghostauthor.knowledge.dto.DocumentMoveRequest;
import com.ghostauthor.knowledge.dto.DocumentReorderRequest;
import com.ghostauthor.knowledge.dto.DocumentShareRequest;
import com.ghostauthor.knowledge.dto.CommentCreateRequest;
import com.ghostauthor.knowledge.dto.CommentResponse;
import com.ghostauthor.knowledge.dto.AttachmentResponse;
import com.ghostauthor.knowledge.dto.AttachmentContentResponse;
import com.ghostauthor.knowledge.entity.DocumentEntity;
import com.ghostauthor.knowledge.repository.DocumentRepository;
import com.ghostauthor.knowledge.service.DocumentService;
import com.ghostauthor.knowledge.service.SearchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final String ROLE_ADMIN = "ADMIN";

    private final DocumentService documentService;
    private final SearchService searchService;
    private final DocumentRepository documentRepository;

    public DocumentController(DocumentService documentService,
                              SearchService searchService,
                              DocumentRepository documentRepository) {
        this.documentService = documentService;
        this.searchService = searchService;
        this.documentRepository = documentRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse create(@Valid @RequestBody DocumentCreateRequest request, HttpServletRequest httpRequest) {
        requireEditorOrAdmin(httpRequest);
        if (!isAdmin(httpRequest) && request.owner() != null && !request.owner().isBlank()) {
            String authUser = authUser(httpRequest);
            if (!authUser.equals(request.owner().trim())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅管理员可指定其他 owner");
            }
        }
        return documentService.create(request);
    }

    @PutMapping("/{slug}")
    public DocumentResponse update(@PathVariable String slug,
                                   @Valid @RequestBody DocumentUpdateRequest request,
                                   HttpServletRequest httpRequest) {
        DocumentEntity doc = requireEditableDocument(slug, httpRequest);
        if (!isAdmin(httpRequest) && request.owner() != null && !request.owner().isBlank()) {
            String owner = normalize(doc.getOwner());
            String nextOwner = request.owner().trim();
            if (!nextOwner.equals(owner)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅管理员可变更 owner");
            }
        }
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
    public void delete(@PathVariable String slug, HttpServletRequest httpRequest) {
        requireEditableDocument(slug, httpRequest);
        documentService.delete(slug);
    }

    @GetMapping("/{slug}/versions")
    public List<DocumentVersionResponse> listVersions(@PathVariable String slug) {
        return documentService.listVersions(slug);
    }

    @PostMapping("/{slug}/versions/{versionNo}/restore")
    public DocumentResponse restoreVersion(@PathVariable String slug,
                                           @PathVariable Integer versionNo,
                                           HttpServletRequest httpRequest) {
        requireEditableDocument(slug, httpRequest);
        return documentService.restoreVersion(slug, versionNo);
    }

    @PatchMapping("/{slug}/move")
    public DocumentResponse move(@PathVariable String slug,
                                 @Valid @RequestBody DocumentMoveRequest request,
                                 HttpServletRequest httpRequest) {
        requireEditableDocument(slug, httpRequest);
        return documentService.move(slug, request);
    }

    @PatchMapping("/{slug}/reorder")
    public DocumentResponse reorder(@PathVariable String slug,
                                    @Valid @RequestBody DocumentReorderRequest request,
                                    HttpServletRequest httpRequest) {
        requireEditableDocument(slug, httpRequest);
        return documentService.reorder(slug, request);
    }

    @PatchMapping("/{slug}/share")
    public DocumentResponse updateShare(@PathVariable String slug,
                                        @RequestBody DocumentShareRequest request,
                                        HttpServletRequest httpRequest) {
        requireEditableDocument(slug, httpRequest);
        return documentService.updateShare(slug, request);
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
    public CommentResponse addComment(@PathVariable String slug,
                                      @Valid @RequestBody CommentCreateRequest request,
                                      HttpServletRequest httpRequest) {
        requireEditableDocument(slug, httpRequest);
        return documentService.addComment(slug, request);
    }

    @DeleteMapping("/{slug}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable String slug,
                              @PathVariable Long commentId,
                              HttpServletRequest httpRequest) {
        requireEditableDocument(slug, httpRequest);
        documentService.deleteComment(slug, commentId);
    }

    @GetMapping("/{slug}/attachments")
    public List<AttachmentResponse> listAttachments(@PathVariable String slug) {
        return documentService.listAttachments(slug);
    }

    @PostMapping("/{slug}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse uploadAttachment(@PathVariable String slug,
                                               @RequestParam("file") MultipartFile file,
                                               HttpServletRequest httpRequest) {
        requireEditableDocument(slug, httpRequest);
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
    public void deleteAttachment(@PathVariable String slug,
                                 @PathVariable Long attachmentId,
                                 HttpServletRequest httpRequest) {
        requireEditableDocument(slug, httpRequest);
        documentService.deleteAttachment(slug, attachmentId);
    }

    @GetMapping("/search")
    public List<DocumentResponse> search(@RequestParam("q") String keyword) {
        return searchService.search(keyword);
    }

    private DocumentEntity requireEditableDocument(String slug, HttpServletRequest request) {
        DocumentEntity doc = documentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (canEditDocument(doc, request)) {
            return doc;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户无编辑权限");
    }

    private boolean canEditDocument(DocumentEntity doc, HttpServletRequest request) {
        if (isAdmin(request)) {
            return true;
        }
        String user = authUser(request);
        if (user.isBlank()) {
            return false;
        }
        String owner = normalize(doc.getOwner());
        Set<String> editors = splitMembers(doc.getEditors());
        if (owner.isBlank() && editors.isEmpty()) {
            return true;
        }
        return user.equals(owner) || editors.contains(user);
    }

    private void requireEditorOrAdmin(HttpServletRequest request) {
        String role = authRole(request);
        if (ROLE_ADMIN.equals(role) || "EDITOR".equals(role)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无写权限");
    }

    private boolean isAdmin(HttpServletRequest request) {
        return ROLE_ADMIN.equals(authRole(request));
    }

    private String authUser(HttpServletRequest request) {
        Object value = request.getAttribute("authUser");
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String authRole(HttpServletRequest request) {
        Object value = request.getAttribute("authRole");
        String role = value == null ? "" : String.valueOf(value).trim().toUpperCase();
        return role.isEmpty() ? ROLE_ADMIN : role;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private Set<String> splitMembers(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter((v) -> !v.isEmpty())
                .collect(Collectors.toSet());
    }
}
