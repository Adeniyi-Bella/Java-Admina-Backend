package com.admina.api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.document")
public record DocumentProcessingProperties(
        long maxFileBytes,
        int maxInFlightDocuments,
        String tempDir,
        String geminiApiKey
) {}
