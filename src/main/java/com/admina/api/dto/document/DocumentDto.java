package com.admina.api.dto.document;

import com.admina.api.model.document.ActionPlanItem;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class DocumentDto {
    private final UUID id;
    private final String targetLanguage;
    private final String title;
    private final String sender;
    private final String receivedDate;
    private final String summary;
    private final String translatedText;
    private final Map<String, String> structuredTranslatedText;
    private final List<ActionPlanItem> actionPlan;
    private final List<ActionPlanTaskDto> actionPlanTasks;
    private final Instant createdAt;
    private final Instant updatedAt;

    @Getter
    @Builder
    public static class ActionPlanTaskDto {
        private final UUID id;
        private final String title;
        private final String dueDate;
        private final boolean completed;
        private final String location;
        private final Instant createdAt;
        private final Instant updatedAt;
    }
}
