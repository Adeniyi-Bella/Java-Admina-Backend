package com.admina.api.dto.user;

import java.util.List;

public record UserWithDocumentsResponseDto(
    UserDto user,
    List<UserDocumentDto> documents
) {
}
