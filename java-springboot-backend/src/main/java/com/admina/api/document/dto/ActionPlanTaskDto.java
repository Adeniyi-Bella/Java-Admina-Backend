package com.admina.api.document.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ActionPlanTaskDto(
        UUID id,
        String title,
        LocalDate dueDate,
        boolean completed,
        String location,
        Instant createdAt,
        Instant updatedAt) {
}
