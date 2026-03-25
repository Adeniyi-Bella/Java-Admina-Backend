package com.admina.api.service.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

@Component
@Slf4j
public class TempFileCleaner {

    private static final String TEMP_DIR = "temp";
    private static final Duration MAX_AGE = Duration.ofHours(24);

    @PostConstruct
    public void cleanupOnStartup() {
        Path dir = Path.of(TEMP_DIR);
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
}
