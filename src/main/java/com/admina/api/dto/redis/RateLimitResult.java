package com.admina.api.dto.redis;

public record RateLimitResult(boolean allowed, int limit, long remaining, long resetSeconds) {
}
