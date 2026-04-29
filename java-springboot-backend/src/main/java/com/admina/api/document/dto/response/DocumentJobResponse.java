package com.admina.api.document.dto.response;

import java.util.UUID;

import com.admina.api.document.enums.DocumentProcessStatus;

public record DocumentJobResponse(UUID docId, DocumentProcessStatus status) {
}
