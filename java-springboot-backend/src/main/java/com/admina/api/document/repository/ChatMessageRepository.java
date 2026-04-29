package com.admina.api.document.repository;

import com.admina.api.document.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findAllByDocumentIdOrderByCreatedAtAscIdAsc(UUID documentId);

    Optional<ChatMessage> findTopByDocumentIdAndRoleOrderByCreatedAtDesc(UUID documentId, String role);
}
