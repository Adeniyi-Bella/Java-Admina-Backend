package com.admina.api.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification")
public record NotificationProperties(
    boolean enabled,
    Resend resend
) {
    public record Resend(String apiKey, String fromEmail) {
    }
}
