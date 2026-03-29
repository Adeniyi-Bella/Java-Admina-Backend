package com.admina.api.dto.document;

import java.util.UUID;

public record DocumentJobMessage(
    UUID docId,
    UUID userId,
    String userEmail,
    String docLanguage,
    String targetLanguage,
    String filePath
) {
}
