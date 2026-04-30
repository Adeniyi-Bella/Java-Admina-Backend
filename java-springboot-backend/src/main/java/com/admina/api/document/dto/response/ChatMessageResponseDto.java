package com.admina.api.document.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponseDto(
        UUID id,
        String role,
        String content,
        Instant createdAt) {
}
