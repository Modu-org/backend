package com.ssafy.modu.domain.notification.controller;

import com.ssafy.modu.domain.notification.service.FcmService;
import com.ssafy.modu.global.auth.security.CustomUserDetails;
import com.ssafy.modu.global.common.ApiResponse;
import com.ssafy.modu.global.common.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fcm/test")
public class FcmTestController {

    private final FcmService fcmService;

    @PostMapping("/me")
    public ResponseEntity<ApiResponse<Void>> sendToMe(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        fcmService.sendToUser(
                userDetails.getUserId(),
                "FCM 테스트",
                "저장된 토큰으로 전송된 테스트 알림입니다."
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        "내 기기로 FCM 테스트 알림 전송 완료",
                        null
                )
        );
    }
}