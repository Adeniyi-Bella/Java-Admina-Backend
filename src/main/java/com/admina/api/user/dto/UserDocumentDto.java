package com.admina.api.user.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.admina.api.document.dto.ActionPlanTaskDto;

public record UserDocumentDto (
     UUID id,
    String title,
    String sender,
    String receivedDate,
    List<ActionPlanTaskDto> actionPlanTasks,
    Instant createdAt,
    Instant updatedAt
) {
}
