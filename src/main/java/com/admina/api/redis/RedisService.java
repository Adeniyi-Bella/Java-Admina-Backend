package com.admina.api.redis;

import com.admina.api.document.dto.response.DocumentStatusResponse;
import com.admina.api.document.dto.response.ChatJobStatusResponse;
import com.admina.api.document.enums.DocumentProcessStatus;
import com.admina.api.filters.rate_limit.RateLimitResult;
import com.admina.api.document.enums.ChatProcessStatus;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface RedisService {
    void setDocumentStatus(UUID docId, DocumentProcessStatus status, String errorMessage);

    Optional<DocumentStatusResponse> getDocumentStatus(UUID docId);

    void setChatJobStatus(UUID chatbotPollingId, UUID docId, ChatProcessStatus status, String errorMessage, String response);

    Optional<ChatJobStatusResponse> getChatJobStatus(UUID chatbotPollingId);

    void blacklistJwt(String jti, Duration ttl);

    boolean isJwtBlacklisted(String jti);

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
