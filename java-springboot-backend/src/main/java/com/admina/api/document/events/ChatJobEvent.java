package com.admina.api.document.events;

import java.util.UUID;

public record ChatJobEvent(
        UUID chatbotPollingId,
        UUID docId,
        String userEmail,
        String prompt,
        UUID userMessageId) {
}
