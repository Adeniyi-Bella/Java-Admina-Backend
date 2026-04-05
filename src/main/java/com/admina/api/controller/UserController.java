package com.admina.api.controller;

import com.admina.api.dto.response.CustomApiResponse;
import com.admina.api.dto.user.UserWithDocumentsResponse;
import com.admina.api.security.AuthenticatedPrincipal;
import com.admina.api.service.auth.AuthService;
import com.admina.api.service.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Controller", description = "APIs for user authentication and management")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    /**
     * POST /api/v1/users/authenticate
     * Authenticates (or registers) a user and returns:
     * - the user
     * - documents derived from the top 3 nearest upcoming incomplete tasks
     * - lean document payload (no summary/translated text fields)
     */
    @PostMapping("/authenticate")
    @Operation(summary = "Authenticate or register user", description = "Authenticates or registers a user using JWT bearer token and returns user details with documents derived from the top 3 nearest upcoming incomplete tasks. The document payload is lean and excludes summary/translated text fields.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully returns an authenticated user"),
            @ApiResponse(responseCode = "201", description = "Successfully created a new user"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CustomApiResponse<UserWithDocumentsResponse>> authenticate(
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedPrincipal principal = authService.extractPrincipal(jwt);
        log.info("Authenticating user oid={}", principal.getOid());
        var authResult = userService.authenticate(principal);
        HttpStatus status = authResult.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(CustomApiResponse.success(authResult.response()));
    }
}
