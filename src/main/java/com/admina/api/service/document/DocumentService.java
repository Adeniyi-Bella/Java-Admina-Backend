package com.admina.api.service.document;

import com.admina.api.dto.document.DocumentCreateRequest;
import com.admina.api.dto.document.DocumentJobResponse;
import com.admina.api.dto.document.DocumentStatusResponse;
import com.admina.api.security.AuthenticatedPrincipal;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    DocumentJobResponse createDocumentJob(AuthenticatedPrincipal principal, MultipartFile file, DocumentCreateRequest request);
    DocumentStatusResponse getJobStatus(java.util.UUID docId);
}
