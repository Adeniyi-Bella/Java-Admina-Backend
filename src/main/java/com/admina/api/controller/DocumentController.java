package com.admina.api.controller;

import com.admina.api.dto.response.CustomApiResponse;
import com.admina.api.dto.document.DocumentCreateRequest;
import com.admina.api.dto.document.DocumentJobResponse;
import com.admina.api.dto.document.DocumentStatusResponse;
import com.admina.api.service.auth.AuthService;
import com.admina.api.service.document.DocumentService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
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
            @Valid @ModelAttribute DocumentCreateRequest request) {
        var principal = authService.extractPrincipal(jwt);
        var result = documentService.createDocumentJob(principal, file, request);
        return ResponseEntity.accepted().body(CustomApiResponse.success(result));
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
    public ResponseEntity<Void> deleteAllDocuments(@AuthenticationPrincipal Jwt jwt) {
        var principal = authService.extractPrincipal(jwt);
        documentService.deleteAllDocuments(principal);
        return ResponseEntity.noContent().build();
    }
}
