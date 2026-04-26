package com.admina.api.filters.rate_limit;

public record RateLimitResult(boolean allowed, int limit, long remaining, long resetSeconds) {
}
