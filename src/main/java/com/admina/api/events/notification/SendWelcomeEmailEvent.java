package com.admina.api.events.notification;

import java.util.UUID;

public record SendWelcomeEmailEvent(
    UUID userId,
    String email,
    String username
) {
}
