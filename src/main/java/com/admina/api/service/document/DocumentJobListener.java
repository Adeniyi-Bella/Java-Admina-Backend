package com.admina.api.service.document;

import com.admina.api.config.RabbitConfig;
import com.admina.api.dto.document.DocumentJobMessage;
import com.admina.api.service.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentJobListener {

    private final RedisService redisService;

    @RabbitListener(queues = RabbitConfig.DOC_QUEUE, containerFactory = "documentListenerContainerFactory")
    public void handle(DocumentJobMessage message) {
        var current = redisService.getDocumentStatus(message.docId());
        if (current.isEmpty()) {
            log.warn("Dropping document job with expired status docId={}", message.docId());
            deleteTempFile(message.filePath());
            redisService.releaseDocumentLock(message.userEmail());
            redisService.releaseDocumentSlot();
            return;
        }
        if ("CANCELLED".equalsIgnoreCase(current.get().status())) {
            log.info("Dropping cancelled document job docId={}", message.docId());
            deleteTempFile(message.filePath());
            redisService.releaseDocumentLock(message.userEmail());
            redisService.releaseDocumentSlot();
            return;
        }
        redisService.setDocumentStatus(message.docId(), "PROCESSING", null);
        try {
            // Placeholder for Gemini call + DB insert.
            // For now, simulate success and clean up the temp file.
            deleteTempFile(message.filePath());
            redisService.setDocumentStatus(message.docId(), "COMPLETED", null);
            redisService.releaseDocumentLock(message.userEmail());
            redisService.releaseDocumentSlot();
        } catch (Exception ex) {
            log.error("Document job failed docId={}", message.docId(), ex);
            redisService.setDocumentStatus(message.docId(), "ERROR", ex.getMessage());
            deleteTempFile(message.filePath());
            redisService.releaseDocumentLock(message.userEmail());
            redisService.releaseDocumentSlot();
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
