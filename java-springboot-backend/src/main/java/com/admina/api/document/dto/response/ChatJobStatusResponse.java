package com.admina.api.document.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.admina.api.document.enums.ChatProcessStatus;

public record ChatJobStatusResponse(
        UUID chatbotPollingId,
        UUID docId,
        ChatProcessStatus status,
        String errorMessage,
        String response,
        Instant updatedAt) {
}
