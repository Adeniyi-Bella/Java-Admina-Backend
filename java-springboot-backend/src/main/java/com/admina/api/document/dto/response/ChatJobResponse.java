package com.admina.api.document.dto.response;

import java.util.UUID;

import com.admina.api.document.enums.ChatProcessStatus;

public record ChatJobResponse(UUID chatbotPollingId, UUID docId, ChatProcessStatus status) {
}
