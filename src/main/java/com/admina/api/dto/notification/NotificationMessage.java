package com.admina.api.dto.notification;

import java.util.UUID;

public record NotificationMessage(
    UUID userId,
    String email,
    String username
) {
}
