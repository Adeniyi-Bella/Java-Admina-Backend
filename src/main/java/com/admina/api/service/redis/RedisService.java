package com.admina.api.service.redis;

import com.admina.api.dto.document.DocumentStatusResponse;

import java.util.Optional;
import java.util.UUID;

public interface RedisService {
    void setDocumentStatus(UUID docId, String status, String errorMessage);
    Optional<DocumentStatusResponse> getDocumentStatus(UUID docId);
    boolean tryAcquireDocumentLock(String userKey);
    void releaseDocumentLock(String userKey);
}
