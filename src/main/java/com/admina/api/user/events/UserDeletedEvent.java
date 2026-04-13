package com.admina.api.user.events;

public record UserDeletedEvent(
        String userOid,
        String email) {
}
