package com.admina.api.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.admina.api.payment.model.ProcessedWebhook;

import java.util.UUID;

@Repository
public interface WebhookRepository extends JpaRepository<ProcessedWebhook, UUID> {
}