package com.admina.api.document.service;

import java.util.UUID;

import com.admina.api.document.dto.ActionPlanTaskDto;
import com.admina.api.document.dto.request.UpdateTaskDto;
import com.admina.api.security.auth.AuthenticatedPrincipal;

public interface ActionPlanTaskService {
    ActionPlanTaskDto addTaskToDocument(AuthenticatedPrincipal principal, UUID docId,
            UpdateTaskDto.AddTaskToDocument request);

    ActionPlanTaskDto updateTaskInDocument(
            AuthenticatedPrincipal principal,
            UUID docId,
            UUID taskId,
            UpdateTaskDto.UpdateExistingTask request);

    void deleteTaskFromDocument(AuthenticatedPrincipal principal, UUID docId, UUID taskId);

}
