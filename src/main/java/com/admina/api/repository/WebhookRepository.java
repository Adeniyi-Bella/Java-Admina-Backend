package com.admina.api.repository;

import com.admina.api.model.webhook.ProcessedWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WebhookRepository extends JpaRepository<ProcessedWebhook, UUID> {
}