package com.admina.api.controller;

import com.admina.api.dto.response.ApiResponse;
import com.admina.api.dto.document.DocumentCreateRequest;
import com.admina.api.dto.document.DocumentJobResponse;
import com.admina.api.dto.document.DocumentStatusResponse;
import com.admina.api.service.auth.AuthService;
import com.admina.api.service.document.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final AuthService authService;
    private final DocumentService documentService;

    @PostMapping(value = "/createDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentJobResponse>> createDocument(
        @AuthenticationPrincipal Jwt jwt,
        @RequestPart("file") MultipartFile file,
        @Valid @ModelAttribute DocumentCreateRequest request
    ) {
        var principal = authService.extractPrincipal(jwt);
        var result = documentService.createDocumentJob(principal, file, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/status/{docId}")
    public ResponseEntity<ApiResponse<DocumentStatusResponse>> getStatus(
        @PathVariable("docId") java.util.UUID docId
    ) {
        var status = documentService.getJobStatus(docId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
