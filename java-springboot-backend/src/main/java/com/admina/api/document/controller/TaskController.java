package com.admina.api.document.controller;

import com.admina.api.document.dto.ActionPlanTaskDto;
import com.admina.api.document.dto.request.UpdateTaskDto;
import com.admina.api.document.service.ActionPlanTaskService;
import com.admina.api.exceptions.CustomApiResponse;
import com.admina.api.security.auth.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/documents/{docId}/tasks")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tasks", description = "APIs for managing action plan tasks within documents")
public class TaskController {

    private final AuthService authService;
    private final ActionPlanTaskService taskService;

    @PostMapping
    @Operation(summary = "Add a task to a document", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Task created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Document does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<CustomApiResponse<ActionPlanTaskDto>> addTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID docId,
            @Valid @RequestBody UpdateTaskDto.AddTaskToDocument request) {
        var principal = authService.extractPrincipal(jwt);
        var created = taskService.addTaskToDocument(principal, docId, request);
        return ResponseEntity.status(201).body(CustomApiResponse.success(created));
    }

    @PatchMapping("/{taskId}")
    @Operation(summary = "Update a task in a document", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Document does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Document or task not found")
    })
    public ResponseEntity<CustomApiResponse<ActionPlanTaskDto>> updateTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID docId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskDto.UpdateExistingTask request) {
        var principal = authService.extractPrincipal(jwt);
        var updated = taskService.updateTaskInDocument(principal, docId, taskId, request);
        return ResponseEntity.ok(CustomApiResponse.success(updated));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Delete a task from a document", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Document does not belong to user"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<Void> deleteTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID docId,
            @PathVariable UUID taskId) {
        var principal = authService.extractPrincipal(jwt);
        taskService.deleteTaskFromDocument(principal, docId, taskId);
        return ResponseEntity.noContent().build();
    }
}
