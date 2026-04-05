package com.admina.api.repository;

import com.admina.api.model.document.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findAllByUserId(UUID userId);
    List<Document> findTop5ByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Document d WHERE d.id = :docId AND d.user.email = :email")
    int deleteByIdAndUserEmail(@Param("docId") UUID docId, @Param("email") String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Document d WHERE d.user.email = :email")
    int deleteAllByUserEmail(@Param("email") String email);
}
