package com.admina.api.user.dto;

import com.admina.api.dto.tasks.ActionPlanTaskDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
