package com.admina.api.dto.user;

public record UserAuthenticationResult(
    UserWithDocumentsResponseDto response,
    boolean created
) {
}
