package com.admina.api.dto.document;

import java.time.Instant;

public record DocumentStatusResponse(String status, String errorMessage, Instant updatedAt) {
}
