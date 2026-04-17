package com.admina.api.document.model;

import com.admina.api.user.model.User;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String targetLanguage;

    @Column
    private String title;

    @Column
    private String sender;

    @Column
    private String receivedDate;

    @Column
    private String summary;

    @Column
    private String translatedText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private Map<String, String> structuredTranslatedText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private List<ActionPlanItem> actionPlan;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ActionPlanTask> actionPlanTasks = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant updatedAt;
}
