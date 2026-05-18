package com.ssafy.modu.global.auth.controller;

import com.ssafy.modu.domain.user.dto.*;
import com.ssafy.modu.domain.user.service.AuthService;
import com.ssafy.modu.domain.user.service.LoginServiceResult;
import com.ssafy.modu.global.common.ApiResponse;
import com.ssafy.modu.global.common.SuccessCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.ssafy.modu.global.util.cookie.CookieUtil.createRefreshTokenCookie;
import static com.ssafy.modu.global.util.cookie.CookieUtil.deleteRefreshTokenCookie;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        SignupResponse response = authService.signup(request);

        return ResponseEntity
                .status(201)
                .body(ApiResponse.success(SuccessCode.CREATED,"회원가입이 완료되었습니다.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginServiceResult result = authService.login(request);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        createRefreshTokenCookie(
                                result.refreshToken(),
                                result.refreshTokenMaxAgeMillis(),
                                cookieSecure,
                                cookieSameSite
                        ).toString()
                )
                .body(ApiResponse.success(SuccessCode.OK,"로그인에 성공했습니다.", result.loginResponse()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        authService.logout(request);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        deleteRefreshTokenCookie(cookieSecure, cookieSameSite).toString()
                )
                .body(ApiResponse.success(SuccessCode.OK,"로그아웃되었습니다."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(HttpServletRequest request) {
        RefreshResponse response = authService.refresh(request);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.OK,"Access Token이 재발급되었습니다.", response)
        );
    }

    @GetMapping("/check-id")
    public ResponseEntity<ApiResponse<CheckIdResponse>> checkId(
            @RequestParam(required = false) String userName
    ) {
        CheckIdResponse response = authService.checkId(userName);

        String message = response.available()
                ? "사용 가능한 아이디입니다."
                : "이미 사용 중인 아이디입니다.";

        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.OK,message, response)
        );
    }
}
