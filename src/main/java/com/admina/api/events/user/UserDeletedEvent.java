package com.admina.api.events.user;

public record UserDeletedEvent(
        String userOid,
        String email) {
}
