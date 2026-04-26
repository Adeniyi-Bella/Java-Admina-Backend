package com.admina.api.user.controller;

import com.admina.api.exceptions.CustomApiResponse;
import com.admina.api.security.auth.AuthService;
import com.admina.api.security.auth.AuthenticatedPrincipal;
import com.admina.api.redis.RedisService;
import com.admina.api.security.JwtTokenUtils;
import com.admina.api.user.service.delete.UserDeleteService;
import com.admina.api.user.dto.UserAuthenticationResult;
import com.admina.api.user.dto.response.UserResponseDto;
import com.admina.api.user.service.UserService;

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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Controller", description = "APIs for user authentication and management")
public class UserController {

    private final UserService userService;
    private final UserDeleteService userDeleteService;
    private final AuthService authService;
    private final RedisService redisService;

    /**
     * POST /api/v1/users/authenticate
     * Authenticates (or registers) a user and returns:
     * - the user
     * - documents derived from the top 3 nearest upcoming incomplete tasks
     * - lean document payload (no summary/translated text fields)
     */
    @PostMapping("/authenticate")
    @Operation(summary = "Authenticate or register user", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully returns an authenticated user"),
            @ApiResponse(responseCode = "201", description = "Successfully created a new user"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CustomApiResponse<UserResponseDto.UserWithDocumentsResponseDto>> authenticate(
            @AuthenticationPrincipal Jwt jwt) {
        AuthenticatedPrincipal principal = authService.extractPrincipal(jwt);
        UserAuthenticationResult authResult = userService.authenticate(principal);
        HttpStatus status = authResult.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(CustomApiResponse.success(authResult.response()));
    }

    @DeleteMapping
    @Operation(summary = "Delete user", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedPrincipal principal = authService.extractPrincipal(jwt);
        userDeleteService.deleteCurrentUser(principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user by blacklisting current JWT", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User logged out successfully"),
            @ApiResponse(responseCode = "400", description = "Token missing jti or expiry"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token")
    })
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        String tokenId = jwt.getClaim("uti");
        if (!StringUtils.hasText(tokenId)) {
            String oid = jwt.getClaim("oid");
            log.warn("Logout called with token missing uti or expiry — skipping blacklist oid={}", oid);
        } else {
            Duration ttl = JwtTokenUtils.remainingLifetime(jwt.getExpiresAt());
            if (!ttl.isZero()) {
                redisService.blacklistJwt(tokenId, ttl);
            }
        }
        return ResponseEntity.noContent().build();
    }
}
