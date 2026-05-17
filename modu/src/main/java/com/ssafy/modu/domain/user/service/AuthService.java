package com.ssafy.modu.domain.user.service;

import com.ssafy.modu.domain.user.dto.*;
import com.ssafy.modu.domain.user.entity.User;
import com.ssafy.modu.domain.user.entity.UserDetail;
import com.ssafy.modu.domain.user.repository.UserRepository;
import com.ssafy.modu.global.auth.jwt.JwtTokenProvider;
import com.ssafy.modu.global.auth.oauth.OAuth2LoginCodeRepository;
import com.ssafy.modu.global.auth.redis.AccessTokenBlacklistRepository;
import com.ssafy.modu.global.auth.redis.RefreshTokenRepository;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

import static com.ssafy.modu.global.util.cookie.CookieUtil.getRefreshToken;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;
    private final OAuth2LoginCodeRepository oAuth2LoginCodeRepository;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByUserNameAndIsDeletedFalse(request.userName())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USER_NAME);
        }

        User user = User.builder()
                .userName(request.userName())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .isDeleted(false)
                .build();

        UserDetail userDetail = UserDetail.builder()
                .physical(defaultFalse(request.physical()))
                .infantFamily(defaultFalse(request.infantFamily()))
                .visual(defaultFalse(request.visual()))
                .hearing(defaultFalse(request.hearing()))
                .build();

        user.setUserDetail(userDetail);

        User savedUser = userRepository.save(user);

        return new SignupResponse(
                savedUser.getId(),
                savedUser.getUserName(),
                savedUser.getNickname()
        );
    }

    @Transactional
    public LoginServiceResult login(LoginRequest request) {
        if (isBlank(request.userName()) || isBlank(request.password())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_REQUEST);
        }

        User user = userRepository.findByUserNameAndIsDeletedFalse(request.userName())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        UserDetail userDetail = user.getUserDetail();

        String sessionId = UUID.randomUUID().toString();
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUserName());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getUserName(), sessionId);

        refreshTokenRepository.save(
                user.getId(),
                sessionId,
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpirationMillis()
        );

        LoginResponse response = new LoginResponse(
                accessToken,
                user.getId(),
                user.getNickname(),
                userDetail.getPhysical(),
                userDetail.getInfantFamily(),
                userDetail.getVisual(),
                userDetail.getHearing()
        );

        return new LoginServiceResult(
                response,
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpirationMillis()
        );
    }

    @Transactional
    public void logout(HttpServletRequest request) {
        String accessToken = resolveAccessToken(request);

        if (accessToken != null) {
            try {
                jwtTokenProvider.validateAccessToken(accessToken);

                long remainingExpirationMillis =
                        jwtTokenProvider.getRemainingExpirationMillis(accessToken);

                accessTokenBlacklistRepository.save(
                        accessToken,
                        remainingExpirationMillis
                );
            } catch (BusinessException e) {
                // 이미 만료되었거나 유효하지 않은 access token은 blacklist에 넣을 필요가 없으므로 무시
            }
        }

        String refreshToken = getRefreshToken(request)
                .orElse(null);

        if (refreshToken == null) {
            return;
        }

        try {
            jwtTokenProvider.validateRefreshToken(refreshToken);

            Long userId = jwtTokenProvider.getUserId(refreshToken);
            String sessionId = jwtTokenProvider.getSessionId(refreshToken);

            String savedRefreshToken = refreshTokenRepository.find(userId, sessionId);

            if (savedRefreshToken != null && savedRefreshToken.equals(refreshToken)) {
                refreshTokenRepository.delete(userId, sessionId);
            }
        } catch (BusinessException e) {
            // 이미 만료되었거나 유효하지 않은 refresh token이면
            // Redis에서 삭제할 수 없으므로 무시하고 로그아웃 응답은 정상 처리
        }
    }

    public RefreshResponse refresh(HttpServletRequest request) {
        String refreshToken = getRefreshToken(request)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_REQUIRED));

        jwtTokenProvider.validateRefreshToken(refreshToken);

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String userName = jwtTokenProvider.getUserName(refreshToken);
        String sessionId = jwtTokenProvider.getSessionId(refreshToken);

        String savedRefreshToken = refreshTokenRepository.find(userId, sessionId);

        if (savedRefreshToken == null) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (!savedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(userId, userName);

        return new RefreshResponse(newAccessToken);
    }

    public CheckIdResponse checkId(String userName) {
        if (isBlank(userName)) {
            throw new BusinessException(ErrorCode.USERNAME_REQUIRED);
        }

        if (!userName.matches("^[a-zA-Z0-9_]{4,30}$")) {
            throw new BusinessException(ErrorCode.INVALID_USERNAME_FORMAT);
        }

        boolean exists = userRepository.existsByUserNameAndIsDeletedFalse(userName);

        return new CheckIdResponse(userName, !exists);
    }

    private Boolean defaultFalse(Boolean value) {
        return value != null && value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authorization)) {
            return null;
        }

        if (!authorization.startsWith("Bearer ")) {
            return null;
        }

        return authorization.substring("Bearer ".length());
    }
    public RefreshResponse exchangeOAuthLoginCode(String code) {
        String accessToken = oAuth2LoginCodeRepository.getAndDelete(code);

        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return new RefreshResponse(accessToken);
    }
}