package com.admina.api.redis;

import java.util.UUID;

public final class RedisKeys {

    private RedisKeys() {
    }

    public static String docJob(UUID docId) {
        return "doc:job:" + docId;
    }

    public static String docLock(String userEmail) {
        return "doc:lock:" + userEmail;
    }

    public static String docCapacity() {
        return "doc:capacity";
    }

    public static String rateLimitIp(String ip) {
        return "rl:ip:" + ip;
    }

    public static String welcomeSent(UUID userId) {
        return "notif:welcome:user:sent:" + userId;
    }

    public static String welcomePending(UUID userId) {
        return "notif:welcome:user:pending:" + userId;
    }
}
