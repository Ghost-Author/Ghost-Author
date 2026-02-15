package com.ghostauthor.knowledge.service;

import com.ghostauthor.knowledge.dto.DocumentCreateRequest;
import com.ghostauthor.knowledge.dto.DocumentResponse;
import com.ghostauthor.knowledge.dto.DocumentUpdateRequest;
import com.ghostauthor.knowledge.dto.DocumentVersionDiffResponse;
import com.ghostauthor.knowledge.dto.DocumentVersionResponse;
import com.ghostauthor.knowledge.dto.CommentCreateRequest;
import com.ghostauthor.knowledge.dto.CommentResponse;
import com.ghostauthor.knowledge.dto.DocumentMoveRequest;
import com.ghostauthor.knowledge.dto.DocumentReorderRequest;
import com.ghostauthor.knowledge.dto.AttachmentResponse;
import com.ghostauthor.knowledge.dto.AttachmentContentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {
    DocumentResponse create(DocumentCreateRequest request);
    DocumentResponse update(String slug, DocumentUpdateRequest request);
    DocumentResponse getBySlug(String slug);
    List<DocumentResponse> list();
    void delete(String slug);
    List<DocumentVersionResponse> listVersions(String slug);
    DocumentResponse restoreVersion(String slug, Integer versionNo);
    DocumentResponse move(String slug, DocumentMoveRequest request);
    DocumentResponse reorder(String slug, DocumentReorderRequest request);
    DocumentVersionDiffResponse diffVersions(String slug, Integer fromVersion, Integer toVersion);
    List<CommentResponse> listComments(String slug);
    CommentResponse addComment(String slug, CommentCreateRequest request);
    void deleteComment(String slug, Long commentId);
    List<AttachmentResponse> listAttachments(String slug);
    AttachmentResponse uploadAttachment(String slug, MultipartFile file);
    AttachmentContentResponse getAttachmentContent(String slug, Long attachmentId);
    void deleteAttachment(String slug, Long attachmentId);
}
