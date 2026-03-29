package com.admina.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ratelimit")
public record AppRateLimitProperties(
    boolean enabled,
    int requestsPerMinute,
    int windowSeconds
) {
}
