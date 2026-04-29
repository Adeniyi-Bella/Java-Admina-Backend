package com.admina.api.security;

import java.time.Duration;
import java.time.Instant;

public final class JwtTokenUtils {

    private JwtTokenUtils() {
    }

    public static Duration remainingLifetime(Instant expiresAt) {
        if (expiresAt == null) {
            return Duration.ZERO;
        }
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        return ttl.isNegative() ? Duration.ZERO : ttl;
    }
}
