package com.admina.api.document.service;

import com.admina.api.ai_models.gemini.dto.SummarizeResponse;
import com.admina.api.ai_models.gemini.dto.TranslateResponse;
import com.admina.api.config.properties.DocumentProcessingProperties;
import com.admina.api.document.dto.request.DocumentCreateRequest;
import com.admina.api.document.dto.request.UpdateTaskDto;
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
import com.admina.api.document.repository.DocumentRepository;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.exceptions.AppExceptions.ForbiddenException;
import com.admina.api.exceptions.AppExceptions.ResourceNotFoundException;
import com.admina.api.redis.RedisService;
import com.admina.api.security.auth.AuthenticatedPrincipal;
import com.admina.api.user.dto.UserDto;
import com.admina.api.user.model.User;
import com.admina.api.user.repository.ActionPlanTaskRepository;
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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final UserService userService;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ActionPlanTaskRepository actionPlanTaskRepository;
    private final EntityManager entityManager;
    private final DocumentJobPublisher jobPublisher;
    private final RedisService redisService;
    private final DocumentProcessingProperties documentProcessingProperties;
    private final TempFileUtils tempFileUtils;

    @Override
    public DocumentJobResponse createDocumentJob(AuthenticatedPrincipal principal, MultipartFile file,
            DocumentCreateRequest request) {
        validateFile(file);
        UserDto user = userService.getExistingUserByEmail(principal.getEmail());

        if (!redisService.tryReserveDocumentSlot(documentProcessingProperties.maxInFlightDocuments())) {
            throw new AppExceptions.TooManyRequestsException("Document queue is full. Please try again later");
        }
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
            filePath = saveTempFile(docId, file);
            redisService.setDocumentStatus(docId, DocumentProcessStatus.PENDING, null);
            jobPublisher.publish(new DocumentCreateEvent(
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

        return new GetDocumentResponseDto(
                document.getId(),
                document.getTargetLanguage(),
                document.getTitle(),
                document.getSender(),
                document.getReceivedDate(),
                document.getSummary(),
                document.getStructuredTranslatedText(),
                document.getActionPlan(),
                actionPlanTasks);
    }

    @Transactional
    @Override
    public ActionPlanTaskDto addTaskToDocument(
            AuthenticatedPrincipal principal,
            UUID docId,
            UpdateTaskDto.AddTaskToDocument request) {

        Document document = documentRepository.findDocumentByIdAndUserEmail(docId, principal.getEmail())
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Document not found"));

        String normalizedTitle = normalizeTitle(request.title());
        String normalizedLocation = normalizeLocation(request.location());

        ActionPlanTask task = ActionPlanTask.builder()
                .document(document)
                .user(document.getUser())
                .title(normalizedTitle)
                .dueDate(request.dueDate() != null ? request.dueDate() : LocalDate.now())
                .completed(false)
                .location(normalizedLocation)
                .build();

        ActionPlanTask saved = actionPlanTaskRepository.save(task);
        return toActionPlanTaskDto(saved);
    }

    @Transactional
    @Override
    public ActionPlanTaskDto updateTaskInDocument(
            AuthenticatedPrincipal principal,
            UUID docId,
            UUID taskId,
            UpdateTaskDto.UpdateExistingTask request) {

        if (!request.hasUpdates()) {
            throw new AppExceptions.BadRequestException("At least one field must be provided for update");
        }

        documentRepository.findDocumentByIdAndUserEmail(docId, principal.getEmail())
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Document not found"));

        ActionPlanTask task = actionPlanTaskRepository.findByIdAndDocumentId(taskId, docId)
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Task not found for document"));

        setTaskProperties(request, task);

        ActionPlanTask updated = actionPlanTaskRepository.save(task);
        return toActionPlanTaskDto(updated);
    }

    private void setTaskProperties(UpdateTaskDto.UpdateExistingTask request, ActionPlanTask task) {
        if (request.title() != null) {
            task.setTitle(normalizeTitle(request.title()));
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        if (request.completed() != null) {
            task.setCompleted(request.completed());
        }
        if (request.location() != null) {
            task.setLocation(normalizeLocation(request.location()));
        }
    }

    @Transactional
    @Override
    public void deleteTaskFromDocument(AuthenticatedPrincipal principal, UUID docId, UUID taskId) {
        int deleted = actionPlanTaskRepository
            .deleteByTaskIdAndDocumentIdAndUserEmail(taskId, docId, principal.getEmail());

    if (deleted == 0) {
        throw new AppExceptions.ResourceNotFoundException("Task not found for document");
    }
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

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppExceptions.BadRequestException("No file uploaded. Please include a PDF, PNG, or JPEG file.");
        }
        if (file.getSize() > documentProcessingProperties.maxFileBytes()) {
            throw new AppExceptions.BadRequestException(
                    "File size exceeds the maximum allowed ("
                            + (documentProcessingProperties.maxFileBytes() / (1024 * 1024)) + "MB)");
        }
        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(4);
            if (!isAllowedFileType(header)) {
                throw new AppExceptions.BadRequestException(
                        "Invalid file type. Only PDF, PNG, or JPEG files are allowed.");
            }
        } catch (AppExceptions.BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AppExceptions.InternalServerErrorException("Could not read uploaded file");
        }
    }

    private boolean isAllowedFileType(byte[] header) {
        if (header.length < 4)
            return false;

        // PDF: starts with %PDF
        if (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46)
            return true;

        // PNG: starts with \x89PNG
        if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47)
            return true;

        // JPEG: starts with \xFF\xD8\xFF
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF)
            return true;

        return false;
    }

    private String saveTempFile(UUID docId, MultipartFile file) {
        try {
            Path dir = Path.of(documentProcessingProperties.tempDir());
            Files.createDirectories(dir);
            Path path = dir.resolve(docId.toString());
            Files.write(path, file.getBytes());
            return path.toString();
        } catch (Exception ex) {
            log.error("Failed to save temp file docId={}", docId, ex);
            throw new AppExceptions.InternalServerErrorException("Failed to save uploaded file");
        }
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

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new AppExceptions.BadRequestException("Task title is required");
        }
        return title.strip();
    }

    private String normalizeLocation(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }

}
