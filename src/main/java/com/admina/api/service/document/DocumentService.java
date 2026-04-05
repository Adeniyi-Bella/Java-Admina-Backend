package com.admina.api.service.document;

import com.admina.api.dto.document.DocumentCreateRequest;
import com.admina.api.dto.document.DocumentJobResponse;
import com.admina.api.dto.document.DocumentStatusResponse;
import com.admina.api.dto.ai.gemini.SummarizeResponse;
import com.admina.api.dto.ai.gemini.TranslateResponse;
import com.admina.api.events.document.DocumentCreateEvent;
import com.admina.api.security.AuthenticatedPrincipal;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

public interface DocumentService {
    DocumentJobResponse createDocumentJob(AuthenticatedPrincipal principal, MultipartFile file,
            DocumentCreateRequest request);

    DocumentStatusResponse getJobStatus(UUID docId);

    void createDocumentAndDecrementLimit(DocumentCreateEvent message, TranslateResponse translated,
            SummarizeResponse summarized);

    void deleteDocumentById(AuthenticatedPrincipal principal, UUID docId);

    void deleteAllDocuments(AuthenticatedPrincipal principal);
}
