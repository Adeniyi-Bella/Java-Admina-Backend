package com.admina.api.user.dto;

import java.time.Instant;
import java.util.UUID;

import com.admina.api.user.enums.PlanType;
import com.admina.api.user.model.UserRole;

public record UserDto(
        UUID id,
        String email,
        String oid,
        String username,
        UserRole role,
        Instant createdAt,
        Instant updatedAt,
        int planLimitCurrent,
        PlanType plan,
        String stripeCustomerId) {
}
