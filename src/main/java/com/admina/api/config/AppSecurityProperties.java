package com.admina.api.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(String issuerUri,
                String audience, List<String> allowedOrigins, List<String> publicEndpoints) {
}
