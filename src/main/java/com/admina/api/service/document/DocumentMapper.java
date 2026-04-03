package com.admina.api.service.document;

import com.admina.api.dto.document.DocumentDto;
import com.admina.api.model.document.ActionPlanTask;
import com.admina.api.model.document.Document;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentDto toDto(Document document) {
        return DocumentDto.builder()
            .id(document.getId())
            .targetLanguage(document.getTargetLanguage())
            .title(document.getTitle())
            .sender(document.getSender())
            .receivedDate(document.getReceivedDate())
            .summary(document.getSummary())
            .translatedText(document.getTranslatedText())
            .structuredTranslatedText(document.getStructuredTranslatedText())
            .actionPlan(document.getActionPlan())
            .actionPlanTasks(document.getActionPlanTasks().stream().map(this::toTaskDto).toList())
            .createdAt(document.getCreatedAt())
            .updatedAt(document.getUpdatedAt())
            .build();
    }

    private DocumentDto.ActionPlanTaskDto toTaskDto(ActionPlanTask task) {
        return DocumentDto.ActionPlanTaskDto.builder()
            .id(task.getId())
            .title(task.getTitle())
            .dueDate(task.getDueDate())
            .completed(task.isCompleted())
            .location(task.getLocation())
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .build();
    }
}
