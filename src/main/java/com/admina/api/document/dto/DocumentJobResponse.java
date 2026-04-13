package com.admina.api.document.dto;

import java.util.UUID;

import com.admina.api.enums.DocumentProcessStatus;

public record DocumentJobResponse(UUID docId, DocumentProcessStatus status) {
}
