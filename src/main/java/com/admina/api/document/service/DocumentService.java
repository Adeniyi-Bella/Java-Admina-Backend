package com.admina.api.document.service;

import com.admina.api.ai_models.gemini.dto.SummarizeResponse;
import com.admina.api.ai_models.gemini.dto.TranslateResponse;
import com.admina.api.document.dto.DocumentCreateRequest;
import com.admina.api.document.dto.DocumentJobResponse;
import com.admina.api.document.dto.DocumentStatusResponse;
import com.admina.api.document.dto.response.GetDocumentResponseDto;
import com.admina.api.document.dto.response.GetDocumentsPageDto;
import com.admina.api.document.events.DocumentCreateEvent;
import com.admina.api.security.auth.AuthenticatedPrincipal;

import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

public interface DocumentService {
    DocumentJobResponse createDocumentJob(AuthenticatedPrincipal principal, MultipartFile file, 
            DocumentCreateRequest request);

    DocumentStatusResponse getJobStatus(UUID docId);

    GetDocumentsPageDto getDocuments(AuthenticatedPrincipal principal, int page, int size);

    GetDocumentResponseDto getDocumentById(AuthenticatedPrincipal principal, UUID docId);

    void createDocumentAndDecrementLimit(DocumentCreateEvent message, TranslateResponse translated,
            SummarizeResponse summarized);

    void deleteDocumentById(AuthenticatedPrincipal principal, UUID id);

    void deleteAllDocuments(AuthenticatedPrincipal principal);

    
}
