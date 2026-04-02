package com.admina.api.events.document;

import java.util.UUID;

public record DocumentCreateEvent(
    UUID docId,
    UUID userId,
    String userEmail,
    String lockToken,
    String docLanguage,
    String targetLanguage,
    String filePath
) {
}
