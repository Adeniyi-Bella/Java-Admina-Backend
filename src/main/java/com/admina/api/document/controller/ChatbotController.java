package com.admina.api.document.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import com.admina.api.document.dto.request.ChatbotPromptRequest;
import com.admina.api.document.dto.response.ChatJobResponse;
import com.admina.api.document.dto.response.ChatJobStatusResponse;
import com.admina.api.exceptions.CustomApiResponse;
import com.admina.api.document.service.ChatbotService;
import com.admina.api.security.auth.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/documents/{docId}/chat")
@RequiredArgsConstructor
@Validated
@Tag(name = "Chatbot", description = "APIs for document chatbot jobs and polling")
public class ChatbotController {

    private final AuthService authService;
    private final ChatbotService chatbotService;

    @PostMapping
    @Operation(summary = "Create chatbot job for a document", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Chat job created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid prompt"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Document not found")
    })
    public ResponseEntity<CustomApiResponse<ChatJobResponse>> chat(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID docId,
            @Valid @RequestBody ChatbotPromptRequest request) {
        var principal = authService.extractPrincipal(jwt);
        ChatJobResponse response = chatbotService.createChatJob(principal, docId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(CustomApiResponse.success(response));
    }

    @GetMapping("/status/{chatbotPollingId}")
    @Operation(summary = "Poll chatbot job status", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Chat job not found")
    })
    public ResponseEntity<CustomApiResponse<ChatJobStatusResponse>> getStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID docId,
            @PathVariable UUID chatbotPollingId) {
        var principal = authService.extractPrincipal(jwt);
        ChatJobStatusResponse response = chatbotService.getChatJobStatus(principal, docId, chatbotPollingId);
        return ResponseEntity.ok(CustomApiResponse.success(response));
    }
}
