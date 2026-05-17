package com.ssafy.modu.global.auth.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OAuth2LoginCodeRepository {

    private static final String PREFIX = "oauth2:login-code:";
    private static final Duration LOGIN_CODE_TTL = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;

    public String save(String accessToken) {
        String code = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                key(code),
                accessToken,
                LOGIN_CODE_TTL
        );

        return code;
    }

    public String getAndDelete(String code) {
        String key = key(code);
        String accessToken = redisTemplate.opsForValue().get(key);

        if (accessToken != null) {
            redisTemplate.delete(key);
        }

        return accessToken;
    }

    private String key(String code) {
        return PREFIX + code;
    }
}