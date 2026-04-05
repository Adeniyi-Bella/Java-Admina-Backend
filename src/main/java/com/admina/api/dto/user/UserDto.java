package com.admina.api.dto.user;

import java.time.Instant;
import java.util.UUID;

import com.admina.api.enums.PlanType;
import com.admina.api.model.user.UserRole;

public record UserDto(
                UUID id,
                String email,
                String oid,
                String username,
                UserRole role,
                Instant createdAt,
                Instant updatedAt,
                int planLimitCurrent,
                PlanType plan) {
}
