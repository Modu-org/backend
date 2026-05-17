package com.ssafy.modu.global.auth.oauth;

import com.ssafy.modu.global.auth.jwt.JwtTokenProvider;
import com.ssafy.modu.global.auth.redis.RefreshTokenRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;

import static com.ssafy.modu.global.util.cookie.CookieUtil.createRefreshTokenCookie;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuth2LoginCodeRepository oAuth2LoginCodeRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        CustomOAuth2UserPrincipal principal =
                (CustomOAuth2UserPrincipal) authentication.getPrincipal();

        Long userId = principal.getUserId();
        String userName = principal.getUserName();

        String sessionId = UUID.randomUUID().toString();

        String accessToken = jwtTokenProvider.createAccessToken(userId, userName);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId, userName, sessionId);

        long refreshTokenExpirationMillis =
                jwtTokenProvider.getRefreshTokenExpirationMillis();

        refreshTokenRepository.save(
                userId,
                sessionId,
                refreshToken,
                refreshTokenExpirationMillis
        );

        String loginCode = oAuth2LoginCodeRepository.save(accessToken);

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                createRefreshTokenCookie(
                        refreshToken,
                        refreshTokenExpirationMillis,
                        cookieSecure,
                        cookieSameSite
                ).toString()
        );

        authorizationRequestRepository.deleteCookie(request, response);

        String targetUrl = UriComponentsBuilder
                .fromUriString(redirectUri)
                .queryParam("code", loginCode)
                .build()
                .toUriString();

        response.sendRedirect(targetUrl);
    }
}