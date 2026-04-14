package com.admina.api.controller;

import com.admina.api.document.controller.DocumentController;
import com.admina.api.document.service.DocumentService;
import com.admina.api.security.auth.AuthService;
import com.admina.api.security.auth.AuthenticatedPrincipal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class DocumentControllerDeleteTest {

    @Mock
    private AuthService authService;

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentController documentController;

    @Test
    void deleteDocument_returnsNoContentAndDelegatesToService() throws Exception {
        UUID docId = UUID.randomUUID();
        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("sub", "user-1"));
        AuthenticatedPrincipal principal = AuthenticatedPrincipal.builder()
                .oid("oid-123")
                .email("user@example.com")
                .username("user")
                .build();

        when(authService.extractPrincipal(eq(jwt))).thenReturn(principal);
        doNothing().when(documentService).deleteDocumentById(any(AuthenticatedPrincipal.class), any(UUID.class));

        ResponseEntity<Void> response = documentController.deleteDocument(jwt, docId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(authService).extractPrincipal(eq(jwt));
        verify(documentService).deleteDocumentById(principal, docId);
    }
}