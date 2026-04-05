package com.admina.api.dto.document;

import com.admina.api.dto.tasks.ActionPlanTaskDto;
import com.admina.api.model.document.ActionPlanItem;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DocumentDto(
        UUID id,
        String targetLanguage,
        String title,
        String sender,
        String receivedDate,
        String summary,
        String translatedText,
        Map<String, String> structuredTranslatedText,
        List<ActionPlanItem> actionPlan,
        List<ActionPlanTaskDto> actionPlanTasks,
        Instant createdAt,
        Instant updatedAt) {

}
