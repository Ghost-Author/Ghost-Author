package com.ghostauthor.knowledge.service;

import com.ghostauthor.knowledge.dto.TemplateCreateRequest;
import com.ghostauthor.knowledge.dto.TemplateResponse;
import com.ghostauthor.knowledge.dto.TemplateUpdateRequest;

import java.util.List;

public interface TemplateService {
    List<TemplateResponse> list();
    TemplateResponse create(TemplateCreateRequest request);
    TemplateResponse update(Long id, TemplateUpdateRequest request);
    void delete(Long id);
}
