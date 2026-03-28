package com.admina.api.service.redis;

import com.admina.api.dto.document.DocumentStatusResponse;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import com.admina.api.dto.redis.RateLimitResult;

public interface RedisService {
    void setDocumentStatus(UUID docId, String status, String errorMessage);
    Optional<DocumentStatusResponse> getDocumentStatus(UUID docId);
    boolean tryAcquireDocumentLock(String userKey);
    void releaseDocumentLock(String userKey);
    RateLimitResult checkRateLimit(String key, int limit, Duration window);
    boolean tryReserveDocumentSlot(int maxDocuments);
    void releaseDocumentSlot();
}
