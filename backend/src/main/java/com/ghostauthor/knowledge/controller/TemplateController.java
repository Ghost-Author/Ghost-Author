package com.ghostauthor.knowledge.controller;

import com.ghostauthor.knowledge.dto.TemplateCreateRequest;
import com.ghostauthor.knowledge.dto.TemplateResponse;
import com.ghostauthor.knowledge.dto.TemplateUpdateRequest;
import com.ghostauthor.knowledge.service.TemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private static final String ROLE_ADMIN = "ADMIN";

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public List<TemplateResponse> list() {
        return templateService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateResponse create(@Valid @RequestBody TemplateCreateRequest request, HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return templateService.create(request);
    }

    @PutMapping("/{id}")
    public TemplateResponse update(@PathVariable Long id,
                                   @Valid @RequestBody TemplateUpdateRequest request,
                                   HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return templateService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        templateService.delete(id);
    }

    private void requireAdmin(HttpServletRequest request) {
        Object roleAttr = request.getAttribute("authRole");
        String role = roleAttr == null ? "" : String.valueOf(roleAttr).trim().toUpperCase();
        if (ROLE_ADMIN.equals(role)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅管理员可管理模板");
    }
}
