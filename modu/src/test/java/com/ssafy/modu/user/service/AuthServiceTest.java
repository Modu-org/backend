package com.ssafy.modu.user.service;

import com.ssafy.modu.domain.user.dto.LoginRequest;
import com.ssafy.modu.domain.user.dto.RefreshResponse;
import com.ssafy.modu.domain.user.dto.SignupRequest;
import com.ssafy.modu.domain.user.dto.SignupResponse;
import com.ssafy.modu.domain.user.entity.User;
import com.ssafy.modu.domain.user.entity.UserDetail;
import com.ssafy.modu.domain.user.enums.UiMode;
import com.ssafy.modu.domain.user.repository.UserRepository;
import com.ssafy.modu.domain.user.service.AuthService;
import com.ssafy.modu.domain.user.service.LoginServiceResult;
import com.ssafy.modu.global.auth.jwt.JwtTokenProvider;
import com.ssafy.modu.global.auth.redis.AccessTokenBlacklistRepository;
import com.ssafy.modu.global.auth.redis.RefreshTokenRepository;
import com.ssafy.modu.global.exception.BusinessException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AccessTokenBlacklistRepository accessTokenBlacklistRepository;

    @InjectMocks
    private AuthService authService;


    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userName("testuser")
                .password("encoded-password")
                .nickname("테스터")
                .isDeleted(false)
                .build();

        ReflectionTestUtils.setField(user, "id", 1L);

        UserDetail userDetail = UserDetail.builder()
                .ageGroupCode(1)
                .tripStyleCode(2)
                .usesWheelchair(false)
                .hasStroller(false)
                .usesWalkingAid(false)
                .hasServiceDog(false)
                .needsVisualAssistance(false)
                .uiMode(UiMode.STANDARD)
                .build();

        user.setUserDetail(userDetail);
    }

    @Test
    @DisplayName("회원가입 시 User와 UserDetail이 함께 생성된다")
    void signup_success() {
        SignupRequest request = new SignupRequest(
                "testuser",
                "password123!",
                "테스터",
                1,
                2,
                true,
                false,
                true,
                false,
                true,
                UiMode.STANDARD
        );

        given(userRepository.existsByUserNameAndIsDeletedFalse("testuser"))
                .willReturn(false);

        given(passwordEncoder.encode("password123!"))
                .willReturn("encoded-password");

        given(userRepository.save(any(User.class)))
                .willAnswer(invocation -> {
                    User savedUser = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedUser, "id", 1L);
                    return savedUser;
                });

        SignupResponse response = authService.signup(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.userName()).isEqualTo("testuser");
        assertThat(response.nickname()).isEqualTo("테스터");

        assertThat(savedUser.getUserDetail()).isNotNull();
        assertThat(savedUser.getUserDetail().getAgeGroupCode()).isEqualTo(1);
        assertThat(savedUser.getUserDetail().getTripStyleCode()).isEqualTo(2);
        assertThat(savedUser.getUserDetail().getUsesWheelchair()).isTrue();
        assertThat(savedUser.getUserDetail().getHasStroller()).isFalse();
        assertThat(savedUser.getUserDetail().getUsesWalkingAid()).isTrue();
        assertThat(savedUser.getUserDetail().getHasServiceDog()).isFalse();
        assertThat(savedUser.getUserDetail().getNeedsVisualAssistance()).isTrue();
        assertThat(savedUser.getUserDetail().getUiMode()).isEqualTo(UiMode.STANDARD);
    }

    @Test
    @DisplayName("같은 유저가 여러 번 로그인하면 sessionId가 다른 refresh token이 각각 저장된다")
    void login_multipleDevice_success() {
        given(userRepository.findByUserNameAndIsDeletedFalse("testuser"))
                .willReturn(Optional.of(user));

        given(passwordEncoder.matches("password123!", "encoded-password"))
                .willReturn(true);

        given(jwtTokenProvider.createAccessToken(eq(1L), eq("testuser")))
                .willReturn("access-token-1")
                .willReturn("access-token-2");

        given(jwtTokenProvider.createRefreshToken(eq(1L), eq("testuser"), anyString()))
                .willAnswer(invocation -> {
                    String sessionId = invocation.getArgument(2);
                    return "refresh-token:" + sessionId;
                });

        given(jwtTokenProvider.getRefreshTokenExpirationMillis())
                .willReturn(1000L * 60 * 60 * 24 * 7);

        LoginServiceResult firstLogin = authService.login(
                new LoginRequest("testuser", "password123!")
        );

        LoginServiceResult secondLogin = authService.login(
                new LoginRequest("testuser", "password123!")
        );

        assertThat(firstLogin.refreshToken()).isNotEqualTo(secondLogin.refreshToken());

        verify(refreshTokenRepository, times(2))
                .save(eq(1L), anyString(), anyString(), anyLong());

        ArgumentCaptor<String> sessionIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(refreshTokenRepository, times(2))
                .save(eq(1L), sessionIdCaptor.capture(), anyString(), anyLong());

        assertThat(sessionIdCaptor.getAllValues())
                .hasSize(2)
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("refresh token이 Redis에 존재하면 access token을 재발급한다")
    void refresh_success() {
        String refreshToken = "refresh-token-1";

        given(jwtTokenProvider.getUserId(refreshToken))
                .willReturn(1L);

        given(jwtTokenProvider.getUserName(refreshToken))
                .willReturn("testuser");

        given(jwtTokenProvider.getSessionId(refreshToken))
                .willReturn("session-1");

        given(refreshTokenRepository.find(1L, "session-1"))
                .willReturn(refreshToken);

        given(jwtTokenProvider.createAccessToken(1L, "testuser"))
                .willReturn("new-access-token");

        RefreshResponse response = authService.refresh(
                requestWithRefreshToken(refreshToken)
        );

        assertThat(response.accessToken()).isEqualTo("new-access-token");

        verify(jwtTokenProvider).validateRefreshToken(refreshToken);
        verify(refreshTokenRepository).find(1L, "session-1");
    }

    @Test
    @DisplayName("access token 없이 로그아웃하면 현재 refresh token만 삭제한다")
    void logout_deleteOnlyCurrentSession() {
        String firstRefreshToken = "refresh-token-1";

        given(jwtTokenProvider.getUserId(firstRefreshToken))
                .willReturn(1L);

        given(jwtTokenProvider.getSessionId(firstRefreshToken))
                .willReturn("session-1");

        given(refreshTokenRepository.find(1L, "session-1"))
                .willReturn(firstRefreshToken);

        authService.logout(requestWithRefreshToken(firstRefreshToken));

        verify(accessTokenBlacklistRepository, never()).save(anyString(), anyLong());
        verify(refreshTokenRepository).delete(1L, "session-1");
        verify(refreshTokenRepository, never()).delete(eq(1L), eq("session-2"));
    }

    @Test
    @DisplayName("로그아웃하면 access token을 blacklist에 저장하고 현재 refresh token만 삭제한다")
    void logout_saveAccessTokenBlacklistAndDeleteCurrentRefreshToken() {
        String accessToken = "access-token-1";
        String refreshToken = "refresh-token-1";

        given(jwtTokenProvider.getRemainingExpirationMillis(accessToken))
                .willReturn(1000L * 60 * 10);

        given(jwtTokenProvider.getUserId(refreshToken))
                .willReturn(1L);

        given(jwtTokenProvider.getSessionId(refreshToken))
                .willReturn("session-1");

        given(refreshTokenRepository.find(1L, "session-1"))
                .willReturn(refreshToken);

        authService.logout(requestWithAccessTokenAndRefreshToken(accessToken, refreshToken));

        verify(jwtTokenProvider).validateAccessToken(accessToken);
        verify(accessTokenBlacklistRepository).save(accessToken, 1000L * 60 * 10);

        verify(jwtTokenProvider).validateRefreshToken(refreshToken);
        verify(refreshTokenRepository).delete(1L, "session-1");
    }

    @Test
    @DisplayName("Redis에 저장된 refresh token과 요청 refresh token이 다르면 재발급에 실패한다")
    void refresh_mismatch_fail() {
        String requestRefreshToken = "refresh-token-request";

        given(jwtTokenProvider.getUserId(requestRefreshToken))
                .willReturn(1L);

        given(jwtTokenProvider.getUserName(requestRefreshToken))
                .willReturn("testuser");

        given(jwtTokenProvider.getSessionId(requestRefreshToken))
                .willReturn("session-1");

        given(refreshTokenRepository.find(1L, "session-1"))
                .willReturn("refresh-token-saved");

        assertThatThrownBy(() ->
                authService.refresh(requestWithRefreshToken(requestRefreshToken))
        ).isInstanceOf(BusinessException.class);
    }

    private HttpServletRequest requestWithRefreshToken(String refreshToken) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", refreshToken));
        return request;
    }

    private HttpServletRequest requestWithAccessTokenAndRefreshToken(
            String accessToken,
            String refreshToken
    ) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + accessToken);
        request.setCookies(new Cookie("refreshToken", refreshToken));
        return request;
    }
}