package com.admina.api.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import com.admina.api.document.dto.response.GetDocumentsPageDto;
import com.admina.api.document.model.Document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

        @Query(value = """
                        SELECT new com.admina.api.document.dto.response.GetDocumentsPageDto$DocumentSummary(
                          d.id,
                          d.title,
                          d.sender,
                          d.receivedDate,
                          COALESCE(SUM(CASE WHEN t.completed = true THEN 1L ELSE 0L END), 0L),
                          COALESCE(SUM(CASE WHEN t.completed = false THEN 1L ELSE 0L END), 0L)
                        )
                        FROM Document d
                        LEFT JOIN d.actionPlanTasks t
                        WHERE d.user.email = :email
                        GROUP BY d.id, d.title, d.sender, d.receivedDate, d.createdAt
                        ORDER BY d.createdAt DESC
                        """, countQuery = """
                        SELECT COUNT(d.id)
                        FROM Document d
                        WHERE d.user.email = :email
                        """)
        Page<GetDocumentsPageDto.DocumentSummary> findDocumentsWithTasksStatus(@Param("email") String email,
                        Pageable pageable);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("DELETE FROM Document d WHERE d.id = :docId AND d.user.email = :email")
        int deleteByIdAndUserEmail(@Param("docId") UUID docId, @Param("email") String email);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("DELETE FROM Document d WHERE d.user.email = :email")
        int deleteAllByUserEmail(@Param("email") String email);

        @Query("""
                        SELECT d FROM Document d
                        LEFT JOIN FETCH d.actionPlanTasks
                        WHERE d.id = :docId AND d.user.email = :email
                        """)
        Optional<Document> findDocumentByIdWithTasks(
                        @Param("docId") UUID docId,
                        @Param("email") String email);

        @Query("""
                        SELECT d FROM Document d
                        JOIN FETCH d.user
                        WHERE d.id = :docId AND d.user.email = :email
                        """)
        Optional<Document> findDocumentByIdAndUserEmail(@Param("docId") UUID docId, @Param("email") String email);

        @Query("""
                        SELECT d.id FROM Document d
                        WHERE d.user.id = :userId
                        ORDER BY d.createdAt DESC
                        """)
        List<UUID> findTopDocumentIdsByUserId(@Param("userId") UUID userId, Pageable pageable);

        @Query("""
                        SELECT DISTINCT d FROM Document d
                        LEFT JOIN FETCH d.actionPlanTasks
                        WHERE d.id IN :documentIds
                        """)
        List<Document> findAllWithTasksByIdIn(@Param("documentIds") List<UUID> documentIds);

        long countByUserId(UUID userId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                        SELECT d FROM Document d
                        JOIN FETCH d.user
                        WHERE d.id = :docId AND d.user.email = :email
                        """)
        Optional<Document> findDocumentByIdAndUserEmailForUpdate(@Param("docId") UUID docId,
                        @Param("email") String email);
}
