package com.admina.api.security;

import com.admina.api.config.properties.AppSecurityProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Component
@Slf4j
public class EntraStartupValidator {

    private final AppSecurityProperties appSecurityProperties;
    private final RestTemplate restTemplate;

    public EntraStartupValidator(
            AppSecurityProperties appSecurityProperties,
            RestTemplateBuilder builder) {
        this.appSecurityProperties = appSecurityProperties;
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    @PostConstruct
    public void validateEntraConnectivity() {
        AppSecurityProperties.Entra entra = appSecurityProperties.entra();
        if (!entra.validateOnStartup()) {
            log.warn("Entra startup validation is disabled by configuration");
            return;
        }
        String token = acquireToken(entra);
        verifyGraphReachability(entra, token);
        log.info("Validated Entra token endpoint and Graph reachability at startup");
    }

    private String acquireToken(AppSecurityProperties.Entra entra) {
        String tokenUrl = entra.loginBaseUrl() + "/" + entra.tenantId() + "/oauth2/v2.0/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", entra.clientId());
        body.add("client_secret", entra.clientSecret());
        body.add("scope", "https://graph.microsoft.com/.default");
        body.add("grant_type", "client_credentials");

        try {
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                    tokenUrl,
                    new HttpEntity<>(body, headers),
                    TokenResponse.class);
            TokenResponse tokenResponse = response.getBody();
            if (tokenResponse == null || tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {
                throw new IllegalStateException("Failed startup validation: Entra token response missing access token");
            }
            return tokenResponse.accessToken();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed startup validation: could not acquire Entra access token", ex);
        }
    }

    private void verifyGraphReachability(AppSecurityProperties.Entra entra, String accessToken) {
        String endpoint = entra.graphBaseUrl() + "/organization?$top=1";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(endpoint, org.springframework.http.HttpMethod.GET, request, String.class);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed startup validation: Graph API is unreachable or unauthorized", ex);
        }
    }

    private record TokenResponse(String access_token) {
        String accessToken() {
            return access_token;
        }
    }
}
