package com.admina.api.service.document;

import com.admina.api.dto.document.DocumentDto;
import com.admina.api.dto.tasks.ActionPlanTaskDto;
import com.admina.api.model.document.Document;
import com.admina.api.model.task.ActionPlanTask;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentDto toDto(Document document) {
        return new DocumentDto(
                document.getId(),
                document.getTargetLanguage(),
                document.getTitle(),
                document.getSender(),
                document.getReceivedDate(),
                document.getSummary(),
                document.getTranslatedText(),
                document.getStructuredTranslatedText(),
                document.getActionPlan(),
                document.getActionPlanTasks().stream().map(this::toTaskDto).toList(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }

    private ActionPlanTaskDto toTaskDto(ActionPlanTask task) {
        return new ActionPlanTaskDto(
                task.getId(),
                task.getTitle(),
                task.getDueDate(),
                task.isCompleted(),
                task.getLocation(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
