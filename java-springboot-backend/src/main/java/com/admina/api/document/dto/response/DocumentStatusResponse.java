package com.admina.api.document.dto.response;

import java.time.Instant;

import com.admina.api.document.enums.DocumentProcessStatus;

public record DocumentStatusResponse(DocumentProcessStatus status, String errorMessage, Instant updatedAt) {
}
