package com.admina.api.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;
import com.admina.api.model.UserRole;

@Getter
@Builder
public class UserDto {
    private final UUID id;
    private final String email;
    private final String oid;
    private final String username;
    private final UserRole role;
    private final Instant createdAt;
    private final Instant updatedAt;
}
