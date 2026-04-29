package com.admina.api.user.dto;

import com.admina.api.user.dto.response.UserResponseDto;

public record UserAuthenticationResult(
        UserResponseDto.UserWithDocumentsResponseDto response,
        boolean created) {
}
