package com.admina.api.redis;

import com.admina.api.document.dto.response.DocumentStatusResponse;
import com.admina.api.document.dto.response.ChatJobStatusResponse;
import com.admina.api.document.enums.DocumentProcessStatus;
import com.admina.api.filters.rate_limit.RateLimitResult;
import com.admina.api.document.enums.ChatProcessStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisServiceImpl implements RedisService {

    private static final Duration DOCUMENT_STATUS_TTL = Duration.ofMinutes(15);
    private static final Duration CHAT_STATUS_TTL = Duration.ofMinutes(15);
    private static final Duration DOCUMENT_LOCK_TTL = Duration.ofMinutes(20);
    private static final Duration DOCUMENT_CAPACITY_TTL = Duration.ofHours(2);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private DefaultRedisScript<List<Long>> rateLimitScript;
    private DefaultRedisScript<Long> documentCapacityReserveScript;
    private DefaultRedisScript<Long> documentCapacityReleaseScript;
    private DefaultRedisScript<Long> documentLockReleaseScript;

    @PostConstruct
    void initScripts() {
        this.rateLimitScript = loadScript("scripts/redis-rate-limit.lua", castListClass());
        this.documentCapacityReserveScript = loadScript("scripts/redis-document-capacity-reserve.lua", Long.class);
        this.documentCapacityReleaseScript = loadScript("scripts/redis-document-capacity-release.lua", Long.class);
        this.documentLockReleaseScript = loadScript("scripts/redis-document-lock-release.lua", Long.class);
    }

    @Override
    public void setDocumentStatus(UUID docId, DocumentProcessStatus status, String errorMessage) {
        String key = RedisKeys.docJob(docId);
        try {
            String json = objectMapper.writeValueAsString(new DocumentStatusResponse(
                    status,
                    errorMessage,
                    Instant.now()));
            redisTemplate.opsForValue().set(key, json, DOCUMENT_STATUS_TTL);
        } catch (Exception ex) {
            log.error("Failed to set document status docId={}", docId, ex);
            throw new IllegalStateException("Failed to set document status", ex);
        }
    }

    @Override
    public Optional<DocumentStatusResponse> getDocumentStatus(UUID docId) {
        String key = RedisKeys.docJob(docId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, DocumentStatusResponse.class));
        } catch (Exception ex) {
            log.error("Corrupted document status in Redis docId={}", docId, ex);
            return Optional.empty();
        }
    }

    @Override
    public void setChatJobStatus(UUID chatbotPollingId, UUID docId, ChatProcessStatus status, String errorMessage,
            String response) {
        String key = RedisKeys.chatJob(chatbotPollingId);
        try {
            String json = objectMapper.writeValueAsString(new ChatJobStatusResponse(
                    chatbotPollingId,
                    docId,
                    status,
                    errorMessage,
                    response,
                    Instant.now()));
            redisTemplate.opsForValue().set(key, json, CHAT_STATUS_TTL);
        } catch (Exception ex) {
            log.error("Failed to set chat job status chatbotPollingId={} docId={}", chatbotPollingId, docId, ex);
            throw new IllegalStateException("Failed to set chat job status", ex);
        }
    }

    @Override
    public Optional<ChatJobStatusResponse> getChatJobStatus(UUID chatbotPollingId) {
        String key = RedisKeys.chatJob(chatbotPollingId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, ChatJobStatusResponse.class));
        } catch (Exception ex) {
            log.error("Corrupted chat job status in Redis chatbotPollingId={}", chatbotPollingId, ex);
            return Optional.empty();
        }
    }

    @Override
    public void blacklistJwt(String jti, Duration ttl) {
        redisTemplate.opsForValue().set(RedisKeys.jwtBlacklist(jti), "1", ttl);
    }

    @Override
    public boolean isJwtBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        Boolean exists = redisTemplate.hasKey(RedisKeys.jwtBlacklist(jti));
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public Optional<String> tryAcquireDocumentLock(String userKey) {
        String key = RedisKeys.docLock(userKey);
        String token = UUID.randomUUID().toString();
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, token, DOCUMENT_LOCK_TTL);
        if (Boolean.TRUE.equals(ok)) {
            return Optional.of(token);
        }
        return Optional.empty();
    }

    @Override
    public void releaseDocumentLock(String userKey, String lockToken) {
        if (lockToken == null || lockToken.isBlank()) {
            return;
        }
        redisTemplate.execute(
                documentLockReleaseScript,
                List.of(RedisKeys.docLock(userKey)),
                lockToken);
    }

    @Override
    public RateLimitResult checkRateLimit(String key, int limit, Duration window) {
        List<Long> result = redisTemplate.execute(
                rateLimitScript,
                List.of(key),
                String.valueOf(window.getSeconds()));

        long current = result == null || result.isEmpty() ? 0L : result.get(0);
        Long rawTtl = result != null && result.size() > 1 ? result.get(1) : null;
        long ttl = rawTtl == null ? window.getSeconds() : rawTtl;
        long remaining = Math.max(0L, limit - current);
        boolean allowed = current <= limit;
        long resetSeconds = ttl >= 0 ? ttl : window.getSeconds();

        return new RateLimitResult(allowed, limit, remaining, resetSeconds);
    }

    @Override
    public boolean tryReserveDocumentSlot(int maxDocuments) {
        Long result = redisTemplate.execute(
                documentCapacityReserveScript,
                List.of(RedisKeys.docCapacity()),
                String.valueOf(maxDocuments),
                String.valueOf(DOCUMENT_CAPACITY_TTL.getSeconds()));
        return result != null && result > 0;
    }

    @Override
    public void releaseDocumentSlot() {
        redisTemplate.execute(
                documentCapacityReleaseScript,
                List.of(RedisKeys.docCapacity()));
    }

    @Override
    public boolean tryAcquireIdempotencyKey(String key, Duration ttl) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }

    @Override
    public boolean hasKey(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void setKeyWithTtl(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }

    private <T> DefaultRedisScript<T> loadScript(String resourcePath, Class<T> resultType) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            String scriptText = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            DefaultRedisScript<T> script = new DefaultRedisScript<>();
            script.setScriptText(scriptText);
            script.setResultType(resultType);
            return script;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load Redis script: " + resourcePath, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<List<Long>> castListClass() {
        return (Class<List<Long>>) (Class<?>) List.class;
    }
}
