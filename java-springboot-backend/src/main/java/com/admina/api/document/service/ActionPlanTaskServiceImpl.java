package com.admina.api.document.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.admina.api.document.dto.ActionPlanTaskDto;
import com.admina.api.document.dto.request.UpdateTaskDto;
import com.admina.api.document.model.ActionPlanTask;
import com.admina.api.document.model.Document;
import com.admina.api.document.repository.DocumentRepository;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.security.auth.AuthenticatedPrincipal;
import com.admina.api.user.repository.ActionPlanTaskRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActionPlanTaskServiceImpl implements ActionPlanTaskService {

    private final DocumentRepository documentRepository;
    private final ActionPlanTaskRepository actionPlanTaskRepository;

    @Transactional
    @Override
    public ActionPlanTaskDto addTaskToDocument(AuthenticatedPrincipal principal,
            UUID docId, UpdateTaskDto.AddTaskToDocument request) {

        Document document = getOwnedDocument(principal, docId);

        ActionPlanTask task = ActionPlanTask.builder()
                .document(document)
                .user(document.getUser())
                .title(normalizeTitle(request.title()))
                .dueDate(request.dueDate() != null ? request.dueDate() : LocalDate.now())
                .completed(false)
                .location(normalizeLocation(request.location()))
                .build();

        return toDto(actionPlanTaskRepository.save(task));
    }

    @Transactional
    @Override
    public ActionPlanTaskDto updateTaskInDocument(AuthenticatedPrincipal principal,
            UUID docId, UUID taskId, UpdateTaskDto.UpdateExistingTask request) {

        if (!request.hasUpdates()) {
            throw new AppExceptions.BadRequestException("At least one field must be provided for update");
        }

        getOwnedDocument(principal, docId);

        ActionPlanTask task = actionPlanTaskRepository.findByIdAndDocumentIdWithUser(taskId, docId)
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Task not found for document"));

        if (request.title() != null)
            task.setTitle(normalizeTitle(request.title()));
        if (request.dueDate() != null)
            task.setDueDate(request.dueDate());
        if (request.completed() != null)
            task.setCompleted(request.completed());
        if (request.location() != null)
            task.setLocation(normalizeLocation(request.location()));

        return toDto(actionPlanTaskRepository.save(task));
    }

    @Transactional
    @Override
    public void deleteTaskFromDocument(AuthenticatedPrincipal principal, UUID docId, UUID taskId) {
        int deleted = actionPlanTaskRepository
                .deleteByTaskIdAndDocumentIdAndUserEmail(taskId, docId, principal.getEmail());
        if (deleted == 0) {
            throw new AppExceptions.ResourceNotFoundException("Task not found for document");
        }
    }

    private Document getOwnedDocument(AuthenticatedPrincipal principal, UUID docId) {
        return documentRepository.findDocumentByIdAndUserEmail(docId, principal.getEmail())
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Document not found"));
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new AppExceptions.BadRequestException("Task title is required");
        }
        return title.strip();
    }

    private String normalizeLocation(String value) {
        if (value == null)
            return null;
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    private ActionPlanTaskDto toDto(ActionPlanTask task) {
        return new ActionPlanTaskDto(task.getId(), task.getTitle(), task.getDueDate(),
                task.isCompleted(), task.getLocation(), task.getCreatedAt(), task.getUpdatedAt());
    }
}
