package com.admina.api.user.dto;

import java.util.List;

public record UserWithDocumentsResponseDto(
    UserDto user,
    List<UserDocumentDto> documents
) {
}
