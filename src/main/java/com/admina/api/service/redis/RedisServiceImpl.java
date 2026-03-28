package com.admina.api.service.redis;

import com.admina.api.dto.document.DocumentStatusResponse;
import com.admina.api.dto.redis.RateLimitResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private static final Duration TTL = Duration.ofMinutes(15);
    private static final Duration LOCK_TTL = Duration.ofMinutes(20);
    private static final Duration DOCUMENT_CAPACITY_TTL = Duration.ofHours(2);
    private static final String DOCUMENT_CAPACITY_KEY = "doc:capacity";
    private static final DefaultRedisScript<Long> DOCUMENT_CAPACITY_RESERVE_SCRIPT = buildDocumentCapacityReserveScript();
    private static final DefaultRedisScript<Long> DOCUMENT_CAPACITY_RELEASE_SCRIPT = buildDocumentCapacityReleaseScript();
    private static final DefaultRedisScript<List<Long>> RATE_LIMIT_SCRIPT = buildRateLimitScript();
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

    @Override
    public RateLimitResult checkRateLimit(String key, int limit, Duration window) {
        List<Long> result = redisTemplate.execute(
            RATE_LIMIT_SCRIPT,
            List.of(key),
            String.valueOf(window.getSeconds())
        );

        long current = result == null || result.isEmpty() ? 0L : result.get(0);
        long ttl = result != null && result.size() > 1 ? result.get(1) : window.getSeconds();
        long remaining = Math.max(0L, limit - current);
        boolean allowed = current <= limit;
        long resetSeconds = ttl >= 0 ? ttl : window.getSeconds();

        return new RateLimitResult(allowed, limit, remaining, resetSeconds);
    }

    @Override
    public boolean tryReserveDocumentSlot(int maxDocuments) {
        Long result = redisTemplate.execute(
            DOCUMENT_CAPACITY_RESERVE_SCRIPT,
            List.of(DOCUMENT_CAPACITY_KEY),
            String.valueOf(maxDocuments),
            String.valueOf(DOCUMENT_CAPACITY_TTL.getSeconds())
        );
        return result != null && result > 0;
    }

    @Override
    public void releaseDocumentSlot() {
        redisTemplate.execute(
            DOCUMENT_CAPACITY_RELEASE_SCRIPT,
            List.of(DOCUMENT_CAPACITY_KEY)
        );
    }

    private String key(UUID docId) {
        return "doc:job:" + docId;
    }

    private String lockKey(String userKey) {
        return "doc:lock:" + userKey;
    }

    private static DefaultRedisScript<List<Long>> buildRateLimitScript() {
        DefaultRedisScript<List<Long>> script = new DefaultRedisScript<>();
        script.setScriptText(
            "local current = redis.call('INCR', KEYS[1])\n" +
            "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end\n" +
            "local ttl = redis.call('TTL', KEYS[1])\n" +
            "return {current, ttl}"
        );
        script.setResultType(castListClass());
        return script;
    }

    private static DefaultRedisScript<Long> buildDocumentCapacityReserveScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
            "local current = tonumber(redis.call('GET', KEYS[1]) or '0')\n" +
            "local maxDocuments = tonumber(ARGV[1])\n" +
            "if current >= maxDocuments then\n" +
            "  return 0\n" +
            "end\n" +
            "current = redis.call('INCR', KEYS[1])\n" +
            "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[2]) end\n" +
            "return 1"
        );
        script.setResultType(Long.class);
        return script;
    }

    private static DefaultRedisScript<Long> buildDocumentCapacityReleaseScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
            "local current = tonumber(redis.call('GET', KEYS[1]) or '0')\n" +
            "if current <= 0 then\n" +
            "  redis.call('DEL', KEYS[1])\n" +
            "  return 0\n" +
            "end\n" +
            "current = redis.call('DECR', KEYS[1])\n" +
            "if current <= 0 then\n" +
            "  redis.call('DEL', KEYS[1])\n" +
            "end\n" +
            "return current"
        );
        script.setResultType(Long.class);
        return script;
    }

    @SuppressWarnings("unchecked")
    private static Class<List<Long>> castListClass() {
        return (Class<List<Long>>) (Class<?>) List.class;
    }
}
