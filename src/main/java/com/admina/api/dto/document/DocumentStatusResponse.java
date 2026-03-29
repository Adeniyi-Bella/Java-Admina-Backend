package com.admina.api.dto.document;

import java.time.Instant;

import com.admina.api.enums.DocumentProcessStatus;

public record DocumentStatusResponse(DocumentProcessStatus status, String errorMessage, Instant updatedAt) {
}
