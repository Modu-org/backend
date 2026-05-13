package com.ssafy.modu.global.auth.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String sessionId, String refreshToken, long expirationMillis) {
        redisTemplate.opsForValue().set(
                key(userId, sessionId),
                refreshToken,
                Duration.ofMillis(expirationMillis)
        );
    }

    public String find(Long userId, String sessionId) {
        return redisTemplate.opsForValue().get(key(userId, sessionId));
    }

    public void delete(Long userId, String sessionId) {
        redisTemplate.delete(key(userId, sessionId));
    }

    private String key(Long userId, String sessionId) {
        return PREFIX + userId + ":" + sessionId;
    }
}
