package com.admina.api.payment.services;

public interface WebhookService {
    void handleStripeWebhook(byte[] payload, String sigHeader);
}