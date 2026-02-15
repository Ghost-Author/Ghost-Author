package com.ghostauthor.knowledge.service.impl;

import com.ghostauthor.knowledge.dto.TemplateCreateRequest;
import com.ghostauthor.knowledge.dto.TemplateResponse;
import com.ghostauthor.knowledge.dto.TemplateUpdateRequest;
import com.ghostauthor.knowledge.entity.TemplateEntity;
import com.ghostauthor.knowledge.repository.TemplateRepository;
import com.ghostauthor.knowledge.service.TemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;

    public TemplateServiceImpl(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> list() {
        return templateRepository.findAll().stream()
                .sorted(Comparator.comparing(TemplateEntity::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TemplateResponse create(TemplateCreateRequest request) {
        TemplateEntity entity = new TemplateEntity();
        entity.setName(request.name().trim());
        entity.setDescription(request.description() == null ? null : request.description().trim());
        entity.setContent(request.content());
        return toResponse(templateRepository.save(entity));
    }

    @Override
    @Transactional
    public TemplateResponse update(Long id, TemplateUpdateRequest request) {
        TemplateEntity entity = templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
        entity.setName(request.name().trim());
        entity.setDescription(request.description() == null ? null : request.description().trim());
        entity.setContent(request.content());
        return toResponse(templateRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found");
        }
        templateRepository.deleteById(id);
    }

    private TemplateResponse toResponse(TemplateEntity entity) {
        return new TemplateResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
