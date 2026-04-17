package com.admina.api.document.controller;

import com.admina.api.document.dto.ActionPlanTaskDto;
import com.admina.api.document.dto.request.DocumentCreateRequest;
import com.admina.api.document.dto.request.UpdateTaskDto;
import com.admina.api.document.dto.response.DocumentJobResponse;
import com.admina.api.document.dto.response.DocumentStatusResponse;
import com.admina.api.document.dto.response.GetDocumentResponseDto;
import com.admina.api.document.dto.response.GetDocumentsPageDto;
import com.admina.api.document.service.DocumentService;
import com.admina.api.exceptions.CustomApiResponse;
import com.admina.api.security.auth.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Document Controller", description = "APIs for document processing and deletion")
public class DocumentController {

        private final AuthService authService;
        private final DocumentService documentService;

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Queue document processing", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "202", description = "Document queued successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid file or request"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "429", description = "Queue full or concurrent processing lock")
        })
        public ResponseEntity<CustomApiResponse<DocumentJobResponse>> createDocument(
                        @AuthenticationPrincipal Jwt jwt,
                        @RequestPart("file") MultipartFile file,
                        @Valid @ModelAttribute DocumentCreateRequest requestBody) {
                var principal = authService.extractPrincipal(jwt);
                var result = documentService.createDocumentJob(principal, file, requestBody);
                return ResponseEntity.accepted().body(CustomApiResponse.success(result));
        }

        
        @PostMapping("/{docId}/tasks")
        @Operation(summary = "Add a new task to a document", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Task created successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid document/task payload"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Document does not belong to the authenticated user"),
                        @ApiResponse(responseCode = "404", description = "Document not found")
        })
        public ResponseEntity<CustomApiResponse<ActionPlanTaskDto>> addTask(
                        @AuthenticationPrincipal Jwt jwt,
                        @PathVariable("docId") UUID docId,
                        @Valid @RequestBody UpdateTaskDto.AddTaskToDocument request) {
                var principal = authService.extractPrincipal(jwt);
                var created = documentService.addTaskToDocument(principal, docId, request);
                return ResponseEntity.status(201).body(CustomApiResponse.success(created));
        }
        
        
        
        @GetMapping("/status/{docId}")
        @Operation(summary = "Get document processing status", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Status returned"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "404", description = "Document job not found")
        })
        public ResponseEntity<CustomApiResponse<DocumentStatusResponse>> getStatus(
                        @PathVariable("docId") UUID docId) {
                var status = documentService.getJobStatus(docId);
                return ResponseEntity.ok(CustomApiResponse.success(status));
        }

        @GetMapping("/{docId}")
        @Operation(summary = "Get a single document by ID", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Document fetched successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid document ID format"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Document does not belong to the authenticated user"),
                        @ApiResponse(responseCode = "404", description = "Document not found")
        })
        public ResponseEntity<CustomApiResponse<GetDocumentResponseDto>> getDocument(
                        @AuthenticationPrincipal Jwt jwt,
                        @PathVariable("docId") UUID docId) {
                var principal = authService.extractPrincipal(jwt);
                var document = documentService.getDocumentById(principal, docId);
                return ResponseEntity.ok(CustomApiResponse.success(document));
        }

        @GetMapping
        @Operation(summary = "Get all documents", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Documents fetched successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid pagination params"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<CustomApiResponse<GetDocumentsPageDto>> getDocuments(
                        @AuthenticationPrincipal Jwt jwt,
                        @RequestParam(defaultValue = "0") @Min(0) int page,
                        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
                var principal = authService.extractPrincipal(jwt);
                var documents = documentService.getDocuments(principal, page, size);
                return ResponseEntity.ok(CustomApiResponse.success(documents));
        }

        @DeleteMapping("/{docId}")
        @Operation(summary = "Delete a single document and its tasks", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Document deleted successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Document does not belong to the authenticated user"),
                        @ApiResponse(responseCode = "404", description = "Document not found")
        })
        public ResponseEntity<Void> deleteDocument(
                        @AuthenticationPrincipal Jwt jwt,
                        @PathVariable("docId") UUID docId) {
                var principal = authService.extractPrincipal(jwt);
                documentService.deleteDocumentById(principal, docId);
                return ResponseEntity.noContent().build();
        }

        @DeleteMapping
        @Operation(summary = "Delete all documents and tasks for the authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "All documents deleted successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<Void> deleteDocuments(@AuthenticationPrincipal Jwt jwt) {
                var principal = authService.extractPrincipal(jwt);
                documentService.deleteAllDocuments(principal);
                return ResponseEntity.noContent().build();
        }

        @PatchMapping("/{docId}/tasks/{taskId}")
        @Operation(summary = "Update a task in a document", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Task updated successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid document/task payload"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Document does not belong to the authenticated user"),
                        @ApiResponse(responseCode = "404", description = "Document or task not found")
        })
        public ResponseEntity<CustomApiResponse<ActionPlanTaskDto>> updateTask(
                        @AuthenticationPrincipal Jwt jwt,
                        @PathVariable("docId") UUID docId,
                        @PathVariable("taskId") UUID taskId,
                        @Valid @RequestBody UpdateTaskDto.UpdateExistingTask request) {
                var principal = authService.extractPrincipal(jwt);
                var updated = documentService.updateTaskInDocument(principal, docId, taskId, request);
                return ResponseEntity.ok(CustomApiResponse.success(updated));
        }

        @DeleteMapping("/{docId}/tasks/{taskId}")
        @Operation(summary = "Delete a task in a document", security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid document/task ID format"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403", description = "Document does not belong to the authenticated user"),
                        @ApiResponse(responseCode = "404", description = "Document or task not found")
        })
        public ResponseEntity<Void> deleteTask(
                        @AuthenticationPrincipal Jwt jwt,
                        @PathVariable("docId") UUID docId,
                        @PathVariable("taskId") UUID taskId) {
                var principal = authService.extractPrincipal(jwt);
                documentService.deleteTaskFromDocument(principal, docId, taskId);
                return ResponseEntity.noContent().build();
        }


}
