package com.ssafy.modu.domain.notification.controller;

import com.ssafy.modu.domain.notification.dto.request.FcmTokenSaveRequest;
import com.ssafy.modu.domain.notification.service.FcmTokenService;
import com.ssafy.modu.global.auth.security.CustomUserDetails;
import com.ssafy.modu.global.common.ApiResponse;
import com.ssafy.modu.global.common.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fcm/tokens")
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> saveToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FcmTokenSaveRequest request
    ) {
        fcmTokenService.saveToken(
                userDetails.getUserId(),
                request.getToken()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        "FCM 토큰 저장 성공",
                        null
                )
        );
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FcmTokenSaveRequest request
    ) {
        fcmTokenService.deactivateToken(
                userDetails.getUserId(),
                request.getToken()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        "FCM 토큰 비활성화 성공",
                        null
                )
        );
    }
}