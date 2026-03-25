package com.admina.api.dto.document;

import java.util.UUID;

public record DocumentJobMessage(
    UUID docId,
    String userEmail,
    String docLanguage,
    String targetLanguage,
    String filePath
) {
}
