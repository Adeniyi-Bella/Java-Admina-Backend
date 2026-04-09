package com.admina.api.service.user.delete;

import com.admina.api.config.properties.AppSecurityProperties;
import com.admina.api.exceptions.AppExceptions;
import com.admina.api.events.user.UserDeletedEvent;
import com.admina.api.repository.UserRepository;
import com.admina.api.security.AuthenticatedPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class UserDeleteServiceImpl implements UserDeleteService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AppSecurityProperties appSecurityProperties;
    private final RestTemplate restTemplate;

    public UserDeleteServiceImpl(
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            AppSecurityProperties appSecurityProperties,
            RestTemplateBuilder builder) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.appSecurityProperties = appSecurityProperties;
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    @Transactional
    public void deleteCurrentUser(AuthenticatedPrincipal principal) {
        int deleted = userRepository.deleteByEmail(principal.getEmail());
        if (deleted == 0) {
            throw new AppExceptions.ResourceNotFoundException("User could not be found");
        }
        eventPublisher.publishEvent(new UserDeletedEvent(principal.getOid(), principal.getEmail()));
    }

    @Override
    public void disableUserInEntra(String userOid) {
        AppSecurityProperties.Entra entra = appSecurityProperties.entra();
        if (isBlank(userOid)) {
            throw new RetryableUserDeleteSyncException("Cannot disable Entra user because userOid is blank");
        }

        String accessToken = acquireAccessToken(entra);
        patchDisableUser(userOid, accessToken, entra);
    }

    private String acquireAccessToken(AppSecurityProperties.Entra entra) {
        String tokenUrl = baseLoginUrl(entra) + "/" + entra.tenantId() + "/oauth2/v2.0/token";
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
            if (tokenResponse == null || isBlank(tokenResponse.accessToken())) {
                throw new RetryableUserDeleteSyncException("Entra token response missing access_token");
            }
            return tokenResponse.accessToken();
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            log.error("Entra token acquisition failed status={} body={}", status, ex.getResponseBodyAsString());
            throw new RetryableUserDeleteSyncException(
                    "Entra token acquisition failure status=" + status, ex);
        } catch (ResourceAccessException ex) {
            throw new RetryableUserDeleteSyncException("Transient Entra token acquisition failure", ex);
        } catch (RestClientException ex) {
            throw new RetryableUserDeleteSyncException("Unexpected Entra token acquisition failure", ex);
        }
    }

    private void patchDisableUser(String userOid, String accessToken, AppSecurityProperties.Entra entra) {
        String endpoint = baseGraphUrl(entra) + "/users/" + userOid;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = Map.of("accountEnabled", false);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.exchange(endpoint, HttpMethod.PATCH, request, Void.class);
            log.info("Disabled user in Entra ID userOid={}", userOid);
        } catch (RestClientResponseException ex) {
            handlePatchFailure(userOid, ex);
        } catch (ResourceAccessException ex) {
            throw new RetryableUserDeleteSyncException("Transient Entra disable failure userOid=" + userOid, ex);
        } catch (RestClientException ex) {
            throw new RetryableUserDeleteSyncException("Unexpected Entra disable failure userOid=" + userOid, ex);
        }
    }

    private void handlePatchFailure(String userOid, RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        if (status == 404) {
            log.error("User not found in Entra ID userOid={} status={} body={}",
                    userOid, status, ex.getResponseBodyAsString());
        } else if (status == 400) {
            log.error("Invalid Entra user identifier or bad request userOid={} status={} body={}",
                    userOid, status, ex.getResponseBodyAsString());
        } else if (status == 401 || status == 403) {
            log.error("Unauthorized to disable user in Entra ID userOid={} status={} body={}",
                    userOid, status, ex.getResponseBodyAsString());
        } else if (isRetryableStatus(status)) {
            log.error("Retryable Entra disable failure userOid={} status={} body={}",
                    userOid, status, ex.getResponseBodyAsString());
        } else {
            log.error("Unexpected Entra disable failure userOid={} status={} body={}",
                    userOid, status, ex.getResponseBodyAsString());
        }
        throw new RetryableUserDeleteSyncException(
                "Entra disable failure userOid=" + userOid + " status=" + status, ex);
    }

    private String baseGraphUrl(AppSecurityProperties.Entra entra) {
        return isBlank(entra.graphBaseUrl()) ? "https://graph.microsoft.com/v1.0" : entra.graphBaseUrl();
    }

    private String baseLoginUrl(AppSecurityProperties.Entra entra) {
        return isBlank(entra.loginBaseUrl()) ? "https://login.microsoftonline.com" : entra.loginBaseUrl();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isRetryableStatus(int status) {
        return status == 429 || status >= 500;
    }

    private record TokenResponse(String access_token) {
        String accessToken() {
            return access_token;
        }
    }
}
