package com.admina.api.sub.document;

import com.admina.api.config.RabbitConfig;
import com.admina.api.enums.DocumentProcessStatus;
import com.admina.api.events.document.DocumentCreateEvent;
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
    public void handle(DocumentCreateEvent message) {
        var current = redisService.getDocumentStatus(message.docId());
        if (current.isEmpty()) {
            log.warn("Dropping document job with expired status docId={}", message.docId());
            cleanup(message);
            return;
        }
        if (current.get().status() == DocumentProcessStatus.CANCELLED) {
            log.info("Dropping cancelled document job docId={}", message.docId());
            cleanup(message);
            return;
        }
        redisService.setDocumentStatus(message.docId(), DocumentProcessStatus.PROCESSING, null);
        try {
            // Placeholder for Gemini call + DB insert.
            // For now, simulate success and clean up the temp file.
            deleteTempFile(message.filePath());
            redisService.setDocumentStatus(message.docId(), DocumentProcessStatus.COMPLETED, null);
        } catch (Exception ex) {
            log.error("Document job failed docId={}", message.docId(), ex);
            redisService.setDocumentStatus(message.docId(), DocumentProcessStatus.ERROR, ex.getMessage());
        } finally {
            cleanup(message);
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

    private void cleanup(DocumentCreateEvent message) {
        deleteTempFile(message.filePath());
        redisService.releaseDocumentLock(message.userEmail());
        redisService.releaseDocumentSlot();
    }
}
