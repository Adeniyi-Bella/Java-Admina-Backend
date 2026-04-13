package com.admina.api.user.dto;

public record UserAuthenticationResult(
    UserWithDocumentsResponseDto response,
    boolean created
) {
}
