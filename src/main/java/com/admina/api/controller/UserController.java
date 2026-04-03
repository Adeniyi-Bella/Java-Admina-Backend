package com.admina.api.controller;

import com.admina.api.dto.response.ApiResponse;
import com.admina.api.dto.user.UserWithDocumentsResponse;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.service.auth.AuthService;
import com.admina.api.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    /**
     * POST /api/user
     * Authenticates (or registers) a user via their JWT bearer token.
     */
    @PostMapping("/authenticate")
    public ResponseEntity<ApiResponse<UserWithDocumentsResponse>> authenticate(
        @AuthenticationPrincipal Jwt jwt,
        HttpServletRequest request
    ) {
        String contentType = request.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            throw new AppExceptions.BadRequestException("Content-Type is not supported");
        }
        AuthenticatedPrincipal principal = authService.extractPrincipal(jwt);
        log.info("Authenticating user oid={}", principal.getOid());
        UserWithDocumentsResponse response = userService.authenticate(principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
