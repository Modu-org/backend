package com.ssafy.modu.global.util.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

public class CookieUtil {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String DEFAULT_SAME_SITE = "Lax";

    private CookieUtil() {
    }

    public static Optional<String> getRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public static ResponseCookie createRefreshTokenCookie(
            String refreshToken,
            long maxAgeMillis,
            boolean secure,
            String sameSite
    ) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(resolveSameSite(sameSite))
                .path("/")
                .maxAge(Duration.ofMillis(maxAgeMillis))
                .build();
    }

    public static ResponseCookie deleteRefreshTokenCookie(boolean secure, String sameSite) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(resolveSameSite(sameSite))
                .path("/")
                .maxAge(0)
                .build();
    }

    private static String resolveSameSite(String sameSite) {
        if (sameSite == null || sameSite.isBlank()) {
            return DEFAULT_SAME_SITE;
        }

        return sameSite;
    }
}
