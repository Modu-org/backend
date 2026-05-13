package com.ssafy.modu.global.auth.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class AccessTokenBlacklistRepository {

    private static final String PREFIX = "blacklist:access:";

    private final StringRedisTemplate redisTemplate;

    public void save(String accessToken, long expirationMillis) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }

        if (expirationMillis <= 0) {
            return;
        }

        redisTemplate.opsForValue().set(
                key(accessToken),
                "logout",
                Duration.ofMillis(expirationMillis)
        );
    }

    public boolean exists(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }

        return Boolean.TRUE.equals(redisTemplate.hasKey(key(accessToken)));
    }

    private String key(String accessToken) {
        return PREFIX + accessToken;
    }
}