package com.admina.api.user.dto.response;

import java.util.List;

import com.admina.api.user.dto.UserDocumentDto;
import com.admina.api.user.enums.PlanType;
import com.admina.api.user.model.UserRole;

public class UserResponseDto {

    public record GetOrCreateUserResponseDto(
            String email,
            String username,
            UserRole role,
            PlanType plan) {
    }

    public record UserWithDocumentsResponseDto(
            UserResponseDto.GetOrCreateUserResponseDto user,
            List<UserDocumentDto> documents) {
    }
}
