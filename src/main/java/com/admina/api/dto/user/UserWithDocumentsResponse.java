package com.admina.api.dto.user;

import com.admina.api.dto.document.DocumentDto;

import java.util.List;

public record UserWithDocumentsResponse(
    UserDto user,
    List<DocumentDto> documents
) {
}
