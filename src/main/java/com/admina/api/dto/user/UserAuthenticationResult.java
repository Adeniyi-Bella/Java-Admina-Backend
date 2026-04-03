package com.admina.api.dto.user;

public record UserAuthenticationResult(
    UserWithDocumentsResponse response,
    boolean created
) {
}
