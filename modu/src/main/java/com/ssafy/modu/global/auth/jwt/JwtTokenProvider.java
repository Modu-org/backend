package com.ssafy.modu.global.auth.jwt;

import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String USER_NAME_CLAIM = "userName";
    private static final String SESSION_ID_CLAIM = "sessionId";

    private final JwtProperties jwtProperties;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String createAccessToken(Long userId, String userName) {
        return createToken(userId, userName, null, jwtProperties.getAccessTokenExpirationMillis());
    }

    public String createRefreshToken(Long userId, String userName, String sessionId) {
        return createToken(userId, userName, sessionId, jwtProperties.getRefreshTokenExpirationMillis());
    }

    private String createToken(Long userId, String userName, String sessionId, long expirationMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(USER_NAME_CLAIM, userName)
                .issuedAt(now)
                .expiration(expiry);

        if (sessionId != null && !sessionId.isBlank()) {
            builder.claim(SESSION_ID_CLAIM, sessionId);
        }

        return builder
                .signWith(secretKey)
                .compact();
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public String getUserName(String token) {
        return parseClaims(token).get(USER_NAME_CLAIM, String.class);
    }

    public String getSessionId(String token) {
        String sessionId = parseClaims(token).get(SESSION_ID_CLAIM, String.class);

        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return sessionId;
    }

    public long getRefreshTokenExpirationMillis() {
        return jwtProperties.getRefreshTokenExpirationMillis();
    }

    public boolean validateAccessToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }
    public long getRemainingExpirationMillis(String token) {
        Date expiration = parseClaims(token).getExpiration();
        long remainingMillis = expiration.getTime() - System.currentTimeMillis();

        return Math.max(remainingMillis, 0);
    }

    /**
     * 기존 코드와의 호환을 위해 남겨둔 메서드입니다.
     * Access Token 검증은 validateAccessToken(), Refresh Token 검증은 validateRefreshToken()을 사용하세요.
     */
    @Deprecated
    public boolean validateToken(String token) {
        return validateRefreshToken(token);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
