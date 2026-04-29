package com.admina.api.document.service;

import com.admina.api.ai_models.gemini.dto.SummarizeResponse;
import com.admina.api.ai_models.gemini.dto.TranslateResponse;
import com.admina.api.ai_models.gemini.service.GeminiService;
import com.admina.api.config.properties.DocumentProcessingProperties;
import com.admina.api.document.dto.request.DocumentCreateRequest;
import com.admina.api.document.dto.ActionPlanTaskDto;
import com.admina.api.document.dto.response.DocumentJobResponse;
import com.admina.api.document.dto.response.DocumentStatusResponse;
import com.admina.api.document.dto.response.GetDocumentResponseDto;
import com.admina.api.document.dto.response.GetDocumentsPageDto;
import com.admina.api.document.enums.DocumentProcessStatus;
import com.admina.api.document.events.DocumentCreateEvent;
import com.admina.api.document.model.ActionPlanTask;
import com.admina.api.document.model.Document;
import com.admina.api.document.pub.DocumentJobPublisher;
import com.admina.api.document.repository.ChatMessageRepository;
import com.admina.api.document.repository.DocumentRepository;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.exceptions.AppExceptions.ForbiddenException;
import com.admina.api.exceptions.AppExceptions.ResourceNotFoundException;
import com.admina.api.redis.RedisService;
import com.admina.api.security.auth.AuthenticatedPrincipal;
import com.admina.api.user.dto.UserDto;
import com.admina.api.user.model.User;
import com.admina.api.user.repository.UserRepository;
import com.admina.api.user.service.UserService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final UserService userService;
    private final ChatMessageRepository chatMessageRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final DocumentJobPublisher documentJobPublisher;
    private final RedisService redisService;
    private final DocumentProcessingProperties documentProcessingProperties;
    private final TempFileUtils tempFileUtils;
    private final FileValidationService fileValidationService;

    @Override
    public DocumentJobResponse createDocumentJob(AuthenticatedPrincipal principal, MultipartFile file,
            DocumentCreateRequest request) {

        if (!redisService.tryReserveDocumentSlot(documentProcessingProperties.maxInFlightDocuments())) {
            throw new AppExceptions.TooManyRequestsException("Document queue is full. Please try again later");
        }

        fileValidationService.validate(file);

        UserDto user = userService.getExistingUserByEmail(principal.getEmail());

        if (user.planLimitCurrent() <= 0) {
            throw new AppExceptions.ForbiddenException(
                    "You have reached your document limit for your current plan");
        }

        Optional<String> lockTokenOpt = redisService.tryAcquireDocumentLock(principal.getEmail());

        if (lockTokenOpt.isEmpty()) {
            redisService.releaseDocumentSlot();
            throw new AppExceptions.TooManyRequestsException("A document is already processing for this user");
        }

        String lockToken = lockTokenOpt.get();
        UUID docId = UUID.randomUUID();
        String filePath = null;
        try {
            filePath = tempFileUtils.saveTempFile(docId, file);
            redisService.setDocumentStatus(docId, DocumentProcessStatus.PENDING, null);
            documentJobPublisher.publish(new DocumentCreateEvent(
                    docId,
                    user.id(),
                    principal.getEmail(),
                    lockToken,
                    request.docLanguage(),
                    request.targetLanguage(),
                    filePath));
            log.info("Queued document processing docId={} userOid={}", docId, principal.getOid());
            return new DocumentJobResponse(docId, DocumentProcessStatus.PENDING);
        } catch (Exception ex) {
            log.error("Failed to queue document docId={} userEmail={}", docId, principal.getEmail(), ex);

            tempFileUtils.deleteQuietly(filePath);
            redisService.releaseDocumentLock(principal.getEmail(), lockToken);
            redisService.releaseDocumentSlot();
            throw new AppExceptions.ServiceUnavailableException("Document processing is unavailable");
        }
    }

    @Override
    public DocumentStatusResponse getJobStatus(UUID docId) {
        return redisService.getDocumentStatus(docId)
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Document job not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public GetDocumentsPageDto getDocuments(AuthenticatedPrincipal principal, int page, int size) {
        if (page < 0) {
            throw new AppExceptions.BadRequestException("page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new AppExceptions.BadRequestException("size must be between 1 and 100");
        }

        var pageable = PageRequest.of(page, size);
        var pagedResult = documentRepository.findDocumentsWithTasksStatus(principal.getEmail(), pageable);
        List<GetDocumentsPageDto.DocumentSummary> items = pagedResult.getContent();

        return new GetDocumentsPageDto(
                items,
                pagedResult.getNumber(),
                pagedResult.getSize(),
                pagedResult.getTotalElements(),
                pagedResult.getTotalPages(),
                pagedResult.hasNext(),
                pagedResult.hasPrevious());
    }

    @Transactional(readOnly = true)
    @Override
    public GetDocumentResponseDto getDocumentById(AuthenticatedPrincipal principal, UUID docId) {

        Document document = documentRepository.findDocumentByIdWithTasks(docId, principal.getEmail())
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Document not found"));

        List<ActionPlanTaskDto> actionPlanTasks = document.getActionPlanTasks().stream()
                .map(this::toActionPlanTaskDto)
                .toList();

        List<GeminiService.ChatHistoryMessage> chatMessagesHistory = chatMessageRepository
                .findAllByDocumentIdOrderByCreatedAtAscIdAsc(docId)
                .stream()
                .map(chat -> new GeminiService.ChatHistoryMessage(chat.getRole(),
                        chat.getContent()))
                .toList();

        return new GetDocumentResponseDto(
                document.getId(),
                document.getTargetLanguage(),
                document.getTitle(),
                document.getSender(),
                document.getReceivedDate(),
                document.getSummary(),
                document.getStructuredTranslatedText(),
                document.getActionPlan(),
                actionPlanTasks,
                chatMessagesHistory);
    }

    @Transactional
    @Override
    public void deleteDocumentById(AuthenticatedPrincipal principal, UUID docId) {
        // check if document exists at all
        if (!documentRepository.existsById(docId)) {
            throw new ResourceNotFoundException("Document not found");
        }
        int deleted = documentRepository.deleteByIdAndUserEmail(docId, principal.getEmail());
        if (deleted == 0) {
            // document exists but doesn't belong to this user
            throw new ForbiddenException("You do not have permission to delete this document");
        }
    }

    @Transactional
    @Override
    public void deleteAllDocuments(AuthenticatedPrincipal principal) {
        documentRepository.deleteAllByUserEmail(principal.getEmail());
    }

    @Transactional
    @Override
    public void createDocumentAndDecrementLimit(
            DocumentCreateEvent message,
            TranslateResponse translated,
            SummarizeResponse summarized) {

        User user = userRepository.getReferenceById(message.userId());
        Document document = Document.builder()
                .id(message.docId())
                .user(user)
                .targetLanguage(message.targetLanguage())
                .title(summarized.title())
                .sender(summarized.sender())
                .receivedDate(summarized.receivedDate())
                .summary(summarized.summary())
                .translatedText(translated.translatedText())
                .structuredTranslatedText(translated.structuredTranslatedText())
                .actionPlan(summarized.actionPlan())
                .build();

        List<ActionPlanTask> tasks = summarized.actionPlans().stream()
                .map(task -> ActionPlanTask.builder()
                        .document(document)
                        .user(user)
                        .title(task.title())
                        .dueDate(task.dueDate())
                        .completed(task.completed())
                        .location(task.location())
                        .build())
                .toList();
        document.setActionPlanTasks(tasks);

        try {
            entityManager.persist(document);
            entityManager.flush();
        } catch (PersistenceException ex) {
            if (isDuplicateDocumentInsert(ex)) {
                log.info("Document already persisted by concurrent worker; skipping duplicate docId={}",
                        message.docId());
                return;
            }
            throw ex;
        }

        int decremented = userRepository.decrementDocumentsUsedIfPositive(message.userId());
        if (decremented == 0) {
            throw new AppExceptions.ConflictException("User has no remaining plan limit");
        }

        log.info("Document created and plan limit decremented userId={} docId={}",
                message.userId(), message.docId());
    }

    private boolean isDuplicateDocumentInsert(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && (message.contains("duplicate key")
                            || message.contains("already exists")
                            || message.contains("pk_documents")
                            || message.contains("documents_pkey"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private ActionPlanTaskDto toActionPlanTaskDto(ActionPlanTask task) {
        return new ActionPlanTaskDto(
                task.getId(),
                task.getTitle(),
                task.getDueDate(),
                task.isCompleted(),
                task.getLocation(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }

}
