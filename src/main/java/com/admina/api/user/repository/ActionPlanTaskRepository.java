package com.admina.api.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.admina.api.user.model.ActionPlanTask;

import java.util.List;
import java.util.UUID;

public interface ActionPlanTaskRepository extends JpaRepository<ActionPlanTask, UUID> {
    List<ActionPlanTask> findAllByDocumentId(UUID documentId);

    List<ActionPlanTask> findTop3ByUserIdAndCompletedFalseOrderByDueDateAsc(UUID userId);
}
