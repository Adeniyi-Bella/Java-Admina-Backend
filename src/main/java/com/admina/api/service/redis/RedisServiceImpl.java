package com.admina.api.service.redis;

import com.admina.api.dto.document.DocumentStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private static final Duration TTL = Duration.ofMinutes(15);
    private static final Duration LOCK_TTL = Duration.ofMinutes(20);
    private final StringRedisTemplate redisTemplate;

    @Override
    public void setDocumentStatus(UUID docId, String status, String errorMessage) {
        String key = key(docId);
        String timestamp = Instant.now().toString();
        redisTemplate.opsForHash().putAll(key, Map.of(
            "status", status,
            "error", errorMessage == null ? "" : errorMessage,
            "updatedAt", timestamp
        ));
        redisTemplate.expire(key, TTL);
    }

    @Override
    public Optional<DocumentStatusResponse> getDocumentStatus(UUID docId) {
        String key = key(docId);
        Object status = redisTemplate.opsForHash().get(key, "status");
        if (status == null) {
            return Optional.empty();
        }
        Object error = redisTemplate.opsForHash().get(key, "error");
        Object updatedAt = redisTemplate.opsForHash().get(key, "updatedAt");
        Instant ts = updatedAt == null ? Instant.now() : Instant.parse(updatedAt.toString());
        String err = error == null || error.toString().isBlank() ? null : error.toString();
        redisTemplate.expire(key, TTL);
        return Optional.of(new DocumentStatusResponse(status.toString(), err, ts));
    }

    @Override
    public boolean tryAcquireDocumentLock(String userKey) {
        String key = lockKey(userKey);
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(
            key,
            "1",
            LOCK_TTL.toMillis(),
            TimeUnit.MILLISECONDS
        );
        return Boolean.TRUE.equals(ok);
    }

    @Override
    public void releaseDocumentLock(String userKey) {
        redisTemplate.delete(lockKey(userKey));
    }

    private String key(UUID docId) {
        return "doc:job:" + docId;
    }

    private String lockKey(String userKey) {
        return "doc:lock:" + userKey;
    }
}
