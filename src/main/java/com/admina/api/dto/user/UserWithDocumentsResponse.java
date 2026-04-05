package com.admina.api.dto.user;

import java.util.List;

public record UserWithDocumentsResponse(
    UserDto user,
    List<UserDocumentDto> documents
) {
}
