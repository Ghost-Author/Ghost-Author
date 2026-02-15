package com.ghostauthor.knowledge.controller;

import com.ghostauthor.knowledge.dto.DocumentCreateRequest;
import com.ghostauthor.knowledge.dto.DocumentResponse;
import com.ghostauthor.knowledge.dto.DocumentUpdateRequest;
import com.ghostauthor.knowledge.dto.DocumentVersionDiffResponse;
import com.ghostauthor.knowledge.dto.DocumentVersionResponse;
import com.ghostauthor.knowledge.service.DocumentService;
import com.ghostauthor.knowledge.service.SearchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{slug}/versions/diff")
    public DocumentVersionDiffResponse diffVersions(@PathVariable String slug,
                                                    @RequestParam("from") Integer fromVersion,
                                                    @RequestParam("to") Integer toVersion) {
        return documentService.diffVersions(slug, fromVersion, toVersion);
    }

    @GetMapping("/search")
    public List<DocumentResponse> search(@RequestParam("q") String keyword) {
        return searchService.search(keyword);
    }
}
