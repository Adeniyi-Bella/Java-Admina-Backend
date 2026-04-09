package com.admina.api.config.properties;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.security")
@Validated
public record AppSecurityProperties(String issuerUri,
                String audience,
                List<String> allowedOrigins,
                List<String> publicEndpoints,
                @NotNull @Valid Entra entra) {
        public record Entra(
                        @NotBlank String tenantId,
                        @NotBlank String clientId,
                        @NotBlank String clientSecret,
                        boolean validateOnStartup,
                        String graphBaseUrl,
                        String loginBaseUrl) {
        }
}
