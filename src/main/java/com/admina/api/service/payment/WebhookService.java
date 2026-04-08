package com.admina.api.service.payment;

public interface WebhookService {
    void handleEvent(byte[] payload, String sigHeader);
}