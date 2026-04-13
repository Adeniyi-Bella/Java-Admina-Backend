package com.admina.api.service.payment;

public interface WebhookService {
    void handleStripeWebhook(byte[] payload, String sigHeader);
}