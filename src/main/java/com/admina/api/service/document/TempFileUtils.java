package com.admina.api.service.document;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.admina.api.config.properties.DocumentProcessingProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class TempFileUtils {

    private static final Duration MAX_AGE = Duration.ofHours(24);
    private final DocumentProcessingProperties documentProcessingProperties;

    @PostConstruct
    public void cleanupOnStartup() {
        Path dir = Path.of(documentProcessingProperties.tempDir());
        if (!Files.exists(dir)) {
            return;
        }

        Instant cutoff = Instant.now().minus(MAX_AGE);
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile).forEach(path -> {
                try {
                    FileTime lastModified = Files.getLastModifiedTime(path);
                    if (lastModified.toInstant().isBefore(cutoff)) {
                        Files.deleteIfExists(path);
                        log.info("Deleted stale temp file {}", path.getFileName());
                    }
                } catch (Exception ex) {
                    log.warn("Failed to inspect/delete temp file {}", path.getFileName(), ex);
                }
            });
        } catch (Exception ex) {
            log.warn("Temp cleanup failed", ex);
        }
    }

    public void deleteQuietly(String filePath) {
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
