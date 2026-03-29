package com.admina.api.repository;

import com.admina.api.model.document.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findAllByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);
}