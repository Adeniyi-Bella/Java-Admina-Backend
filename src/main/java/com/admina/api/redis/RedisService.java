package com.admina.api.redis;

import com.admina.api.document.dto.DocumentStatusResponse;
import com.admina.api.dto.redis.RateLimitResult;
import com.admina.api.enums.DocumentProcessStatus;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface RedisService {
    void setDocumentStatus(UUID docId, DocumentProcessStatus status, String errorMessage);

    Optional<DocumentStatusResponse> getDocumentStatus(UUID docId);

    Optional<String> tryAcquireDocumentLock(String userKey);

    void releaseDocumentLock(String userKey, String lockToken);

    RateLimitResult checkRateLimit(String key, int limit, Duration window);

    boolean tryReserveDocumentSlot(int maxDocuments);

    void releaseDocumentSlot();

    boolean tryAcquireIdempotencyKey(String key, Duration ttl);

    boolean hasKey(String key);

    void setKeyWithTtl(String key, String value, Duration ttl);

    void deleteKey(String key);

}
