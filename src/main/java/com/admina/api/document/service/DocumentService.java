package com.admina.api.document.service;

import com.admina.api.ai_models.gemini.dto.SummarizeResponse;
import com.admina.api.ai_models.gemini.dto.TranslateResponse;
import com.admina.api.document.dto.ActionPlanTaskDto;
import com.admina.api.document.dto.request.DocumentCreateRequest;
import com.admina.api.document.dto.request.UpdateTaskDto;
import com.admina.api.document.dto.response.DocumentJobResponse;
import com.admina.api.document.dto.response.DocumentStatusResponse;
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

        ActionPlanTaskDto addTaskToDocument(AuthenticatedPrincipal principal, UUID docId,
                        UpdateTaskDto.AddTaskToDocument request);

        ActionPlanTaskDto updateTaskInDocument(
                        AuthenticatedPrincipal principal,
                        UUID docId,
                        UUID taskId,
                        UpdateTaskDto.UpdateExistingTask request);

        void deleteTaskFromDocument(AuthenticatedPrincipal principal, UUID docId, UUID taskId);

        void createDocumentAndDecrementLimit(DocumentCreateEvent message, TranslateResponse translated,
                        SummarizeResponse summarized);

        void deleteDocumentById(AuthenticatedPrincipal principal, UUID docId);

        void deleteAllDocuments(AuthenticatedPrincipal principal);

}
