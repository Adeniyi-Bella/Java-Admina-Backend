package com.admina.api.service.notification;

import com.admina.api.dto.notification.NotificationProperties;
import com.admina.api.model.user.User;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class ResendNotificationService implements NotificationService {

    private static final String RESEND_ENDPOINT = "https://api.resend.com/emails";

    private final NotificationProperties notificationProperties;
    private final RestTemplate restTemplate;

    public ResendNotificationService(NotificationProperties notificationProperties, RestTemplateBuilder builder) {
        this.notificationProperties = notificationProperties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = builder.requestFactory(() -> factory).build();
    }

    @Override
    public void sendWelcomeEmail(User user) {
        if (!notificationProperties.enabled()) {
            log.info("Notifications disabled; skipping welcome email for userId={}", user.getId());
            return;
        }

        NotificationProperties.Resend resend = notificationProperties.resend();
        if (resend == null || isBlank(resend.apiKey()) || isBlank(resend.fromEmail())) {
            log.warn("Resend is not configured; skipping welcome email for userId={}", user.getId());
            return;
        }

        Map<String, Object> payload = Map.of(
            "from", resend.fromEmail(),
            "to", user.getEmail(),
            "subject", "Welcome to Admina",
            "html", "<p>Welcome to Admina, " + user.getUsername() + ".</p>"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(resend.apiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                RESEND_ENDPOINT,
                new HttpEntity<>(payload, headers),
                String.class
            );
            HttpStatusCode status = response.getStatusCode();
            if (status.is2xxSuccessful()) {
                log.info("Sent welcome email to userId={} status={}", user.getId(), status);
                return;
            }
            if (isRetryableStatus(status)) {
                throw new RetryableNotificationException("Retryable email error status=" + status);
            }
            log.warn("Non-retryable email error userId={} status={} body={}", user.getId(), status, response.getBody());
        } catch (RestClientException ex) {
            throw new RetryableNotificationException("Transient email send failure", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isRetryableStatus(HttpStatusCode status) {
        return status.is5xxServerError() || status.value() == 429;
    }
}
