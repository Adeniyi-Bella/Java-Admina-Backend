package com.admina.api.notifications;

import java.util.UUID;

public record NotificationMessage(
    UUID userId,
    String email,
    String username
) {
}
