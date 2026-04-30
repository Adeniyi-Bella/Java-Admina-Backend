package com.admina.api.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.admina.api.document.model.ActionPlanTask;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActionPlanTaskRepository extends JpaRepository<ActionPlanTask, UUID> {
    List<ActionPlanTask> findAllByDocumentId(UUID documentId);

    List<ActionPlanTask> findTop3ByUserIdAndCompletedFalseOrderByDueDateAsc(UUID userId);

    List<ActionPlanTask> findTop3ByUserIdAndCompletedFalseAndDueDateIsNotNullOrderByDueDateAscCreatedAtAsc(UUID userId);

    long countByUserIdAndCompletedFalse(UUID userId);

    long countByUserIdAndCompletedTrue(UUID userId);

    @Query("""
                SELECT COUNT(t) FROM ActionPlanTask t
                WHERE t.user.id = :userId
                  AND t.completed = false
                  AND t.dueDate BETWEEN :startDate AND :endDate
            """)
    long countUpcomingByUserId(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
                SELECT t FROM ActionPlanTask t
                JOIN FETCH t.document d
                JOIN FETCH d.user
                WHERE t.id = :taskId AND t.document.id = :documentId
            """)
    Optional<ActionPlanTask> findByIdAndDocumentIdWithUser(
            @Param("taskId") UUID taskId,
            @Param("documentId") UUID documentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                DELETE FROM ActionPlanTask t
                WHERE t.id = :taskId
                  AND t.document.id = :docId
                  AND t.document.user.email = :email
            """)
    int deleteByTaskIdAndDocumentIdAndUserEmail(@Param("taskId") UUID taskId, @Param("docId") UUID documentId,
            @Param("email") String email);
}
