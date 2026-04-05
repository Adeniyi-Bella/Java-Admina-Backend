package com.admina.api.service.document;

import com.admina.api.config.document.DocumentProcessingProperties;
import com.admina.api.dto.ai.gemini.SummarizeResponse;
import com.admina.api.dto.ai.gemini.TranslateResponse;
import com.admina.api.dto.document.DocumentCreateRequest;
import com.admina.api.dto.document.DocumentJobResponse;
import com.admina.api.dto.document.DocumentStatusResponse;
import com.admina.api.dto.user.UserDto;
import com.admina.api.enums.DocumentProcessStatus;
import com.admina.api.events.document.DocumentCreateEvent;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.exceptions.AppExceptions.ForbiddenException;
import com.admina.api.exceptions.AppExceptions.ResourceNotFoundException;
import com.admina.api.model.document.Document;
import com.admina.api.model.task.ActionPlanTask;
import com.admina.api.model.user.User;
import com.admina.api.pub.document.DocumentJobPublisher;
import com.admina.api.redis.RedisService;
import com.admina.api.repository.DocumentRepository;
import com.admina.api.repository.UserRepository;
import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.service.user.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

        int decremented = userRepository.decrementPlanLimitIfPositive(message.userId());
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
            throw new AppExceptions.BadRequestException("File size exceeds the maximum allowed (6MB)");
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

}
