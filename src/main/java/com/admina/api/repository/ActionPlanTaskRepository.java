package com.admina.api.repository;

import com.admina.api.model.task.ActionPlanTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActionPlanTaskRepository extends JpaRepository<ActionPlanTask, UUID> {
    List<ActionPlanTask> findAllByDocumentId(UUID documentId);

    List<ActionPlanTask> findTop3ByUserIdAndCompletedFalseOrderByDueDateAsc(UUID userId);
}
