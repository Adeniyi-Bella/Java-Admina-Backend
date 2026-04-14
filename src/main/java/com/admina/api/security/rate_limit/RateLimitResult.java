package com.admina.api.security.rate_limit;

public record RateLimitResult(boolean allowed, int limit, long remaining, long resetSeconds) {
}
