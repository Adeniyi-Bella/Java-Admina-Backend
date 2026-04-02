package com.admina.api.sub.document;

import com.admina.api.config.RabbitConfig;
import com.admina.api.enums.DocumentProcessStatus;
import com.admina.api.events.document.DocumentCreateEvent;
import com.admina.api.redis.RedisService;
import com.admina.api.service.ai.gemini.GeminiService;
import com.admina.api.service.document.DocumentPersistenceService;
import com.admina.api.dto.ai.gemini.TranslateResponse;
import com.admina.api.dto.ai.gemini.SummarizeResponse;
import com.admina.api.service.document.TempFileUtils;
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
    private final GeminiService geminiService;
    private final DocumentPersistenceService documentPersistenceService;
    private final TempFileUtils tempFileUtils;

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

        try {
            // Step 1: Mark as QUEUE
            redisService.setDocumentStatus(message.docId(), DocumentProcessStatus.QUEUE, null);
            log.info("Processing document docId={}", message.docId());

            // Step 2: Read file bytes
            byte[] fileBytes = Files.readAllBytes(Path.of(message.filePath()));
            String mimeType = detectMimeType(fileBytes);

            // Step 3: Translate
            redisService.setDocumentStatus(message.docId(), DocumentProcessStatus.TRANSLATE, null);
            log.info("Translating document docId={}", message.docId());
            TranslateResponse translated = geminiService.translateDocument(
                    fileBytes,
                    mimeType,
                    message.targetLanguage());

            // Step 4: Summarize
            redisService.setDocumentStatus(message.docId(), DocumentProcessStatus.SUMMARIZE, null);
            log.info("Summarizing document docId={}", message.docId());
            SummarizeResponse summarized = geminiService.summarizeDocument(
                    translated.translatedText(),
                    message.targetLanguage());

            // Step 5: Save to DB
            redisService.setDocumentStatus(message.docId(), DocumentProcessStatus.SAVING, null);
            log.info("Saving document docId={}", message.docId());
            documentPersistenceService.createDocumentAndDecrementLimit(message, translated, summarized);

            // Step 6: Done
            redisService.setDocumentStatus(message.docId(), DocumentProcessStatus.COMPLETED, null);
            log.info("Document processing completed docId={}", message.docId());

        } catch (Exception ex) {
            log.error("Document job failed docId={}", message.docId(), ex);
            redisService.setDocumentStatus(message.docId(), DocumentProcessStatus.ERROR, ex.getMessage());
        } finally {
            cleanup(message);
        }
    }

    private String detectMimeType(byte[] header) {
        if (header.length < 4)
            return "application/octet-stream";

        // PDF
        if (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46)
            return "application/pdf";

        // PNG
        if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47)
            return "image/png";

        // JPEG
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF)
            return "image/jpeg";

        return "application/octet-stream";
    }

    private void cleanup(DocumentCreateEvent message) {
        tempFileUtils.deleteQuietly(message.filePath());
        redisService.releaseDocumentLock(message.userEmail(), message.lockToken());
        redisService.releaseDocumentSlot();
    }
}
