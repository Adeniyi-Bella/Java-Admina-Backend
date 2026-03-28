package com.admina.api.service.document;

import com.admina.api.dto.document.DocumentCreateRequest;
import com.admina.api.dto.document.DocumentJobResponse;
import com.admina.api.dto.document.DocumentStatusResponse;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.dto.document.DocumentJobMessage;
import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.service.redis.RedisService;
import com.admina.api.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private static final long MAX_FILE_BYTES = 6L * 1024 * 1024;
    private static final int MAX_IN_FLIGHT_DOCUMENTS = 20;
    private static final String TEMP_DIR = "temp";

    private final UserService userService;
    private final DocumentJobPublisher jobPublisher;
    private final RedisService redisService;

    @Override
    public DocumentJobResponse createDocumentJob(AuthenticatedPrincipal principal, MultipartFile file, DocumentCreateRequest request) {
        validateFile(file);
        userService.getExistingUserByEmail(principal.getEmail());
        if (!redisService.tryReserveDocumentSlot(MAX_IN_FLIGHT_DOCUMENTS)) {
            throw new AppExceptions.TooManyRequestsException("Document queue is full. Please try again later");
        }
        if (!redisService.tryAcquireDocumentLock(principal.getEmail())) {
            redisService.releaseDocumentSlot();
            throw new AppExceptions.TooManyRequestsException("A document is already processing for this user");
        }
        UUID docId = UUID.randomUUID();
        String filePath = null;
        try {
            filePath = saveTempFile(docId, file);
            redisService.setDocumentStatus(docId, "PENDING", null);
            jobPublisher.publish(new DocumentJobMessage(
                docId,
                principal.getEmail(),
                request.docLanguage(),
                request.targetLanguage(),
                filePath
            ));
            log.info("Queued document processing docId={} userOid={}", docId, principal.getOid());
            return new DocumentJobResponse(docId, "PENDING");
        } catch (Exception ex) {
            deleteTempFile(filePath);
            redisService.releaseDocumentLock(principal.getEmail());
            redisService.releaseDocumentSlot();
            throw new AppExceptions.ServiceUnavailableException("Document processing is unavailable");
        }
    }

    @Override
    public DocumentStatusResponse getJobStatus(UUID docId) {
        return redisService.getDocumentStatus(docId)
            .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Document job not found"));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppExceptions.BadRequestException("No file uploaded. Please include a PDF, PNG, or JPEG file.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new AppExceptions.BadRequestException("File size exceeds the maximum allowed (6MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new AppExceptions.BadRequestException("Invalid file type. Only PDF, PNG, or JPEG files are allowed.");
        }
    }

    private String saveTempFile(UUID docId, MultipartFile file) {
        try {
            Path dir = Path.of(TEMP_DIR);
            Files.createDirectories(dir);
            Path path = dir.resolve(docId.toString());
            Files.write(path, file.getBytes());
            return path.toString();
        } catch (Exception ex) {
            throw new AppExceptions.BadRequestException("Failed to save uploaded file");
        }
    }

    private void deleteTempFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (Exception ex) {
            log.warn("Failed to delete temp file {}", filePath, ex);
        }
    }

    private boolean isAllowedContentType(String contentType) {
        return contentType.equals("application/pdf")
            || contentType.equals("image/png")
            || contentType.equals("image/jpeg")
            || contentType.equals("image/jpg");
    }
}
