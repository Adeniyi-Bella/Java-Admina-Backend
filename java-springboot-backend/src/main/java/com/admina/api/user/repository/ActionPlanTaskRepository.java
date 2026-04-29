package com.admina.api.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.admina.api.document.model.ActionPlanTask;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActionPlanTaskRepository extends JpaRepository<ActionPlanTask, UUID> {
    List<ActionPlanTask> findAllByDocumentId(UUID documentId);

    List<ActionPlanTask> findTop3ByUserIdAndCompletedFalseOrderByDueDateAsc(UUID userId);

    Optional<ActionPlanTask> findByIdAndDocumentId(UUID taskId, UUID documentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM ActionPlanTask t
        WHERE t.id = :taskId
          AND t.document.id = :docId
          AND t.document.user.email = :email
    """)
    int deleteByTaskIdAndDocumentIdAndUserEmail(@Param("taskId") UUID taskId, @Param("docId") UUID documentId, @Param("email") String email);
}
