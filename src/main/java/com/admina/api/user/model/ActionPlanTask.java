package com.admina.api.user.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.admina.api.document.model.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "action_plan_tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionPlanTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column
    private String title;

    @Column
    private LocalDate dueDate;

    @Column
    private boolean completed;

    @Column
    private String location;

    @CreationTimestamp
    @Column(updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    private void validateOwnershipConsistency() {
        if (document == null || user == null || document.getUser() == null) {
            return;
        }
        UUID documentUserId = document.getUser().getId();
        UUID taskUserId = user.getId();
        if (documentUserId != null && taskUserId != null && !documentUserId.equals(taskUserId)) {
            throw new IllegalStateException("Task user_id must match document.user_id");
        }
    }
}
