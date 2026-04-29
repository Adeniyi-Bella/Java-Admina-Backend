package com.admina.api.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "webhook_events")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedWebhook {

    @Id
    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ProcessedWebhook(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
    }
}