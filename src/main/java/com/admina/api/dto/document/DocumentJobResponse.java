package com.admina.api.dto.document;

import java.util.UUID;

import com.admina.api.enums.DocumentProcessStatus;

public record DocumentJobResponse(UUID docId, DocumentProcessStatus status) {
}
