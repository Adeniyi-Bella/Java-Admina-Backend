package com.admina.api.service.document;

import com.admina.api.config.document.DocumentProcessingProperties;
import com.admina.api.dto.document.DocumentCreateRequest;
import com.admina.api.dto.document.DocumentJobResponse;
import com.admina.api.dto.document.DocumentStatusResponse;
import com.admina.api.dto.user.UserDto;
import com.admina.api.enums.DocumentProcessStatus;
import com.admina.api.events.document.DocumentCreateEvent;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.pub.document.DocumentJobPublisher;
import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.service.redis.RedisService;
import com.admina.api.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final UserService userService;
    private final DocumentJobPublisher jobPublisher;
    private final RedisService redisService;
    private final DocumentProcessingProperties documentProcessingProperties;

    @Override
    public DocumentJobResponse createDocumentJob(AuthenticatedPrincipal principal, MultipartFile file,
            DocumentCreateRequest request) {
        validateFile(file);
        UserDto user = userService.getExistingUserByEmail(principal.getEmail());
        if (!redisService.tryReserveDocumentSlot(documentProcessingProperties.maxInFlightDocuments())) {
            throw new AppExceptions.TooManyRequestsException("Document queue is full. Please try again later");
        }
        try {
            if (!redisService.tryAcquireDocumentLock(principal.getEmail())) {
                throw new AppExceptions.TooManyRequestsException("A document is already processing for this user");
            }
        } catch (Exception ex) {
            redisService.releaseDocumentSlot();
            throw ex;
        }
        UUID docId = UUID.randomUUID();
        String filePath = null;
        try {
            filePath = saveTempFile(docId, file);
            redisService.setDocumentStatus(docId, DocumentProcessStatus.PENDING, null);
            jobPublisher.publish(new DocumentCreateEvent(
                    docId,
                    user.getId(),
                    principal.getEmail(),
                    request.docLanguage(),
                    request.targetLanguage(),
                    filePath));
            log.info("Queued document processing docId={} userOid={}", docId, principal.getOid());
            return new DocumentJobResponse(docId, DocumentProcessStatus.PENDING);
        } catch (Exception ex) {
            log.error("Failed to queue document docId={} userEmail={}", docId, principal.getEmail(), ex);

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

}
