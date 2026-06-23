package com.ssafy.modu.domain.notification.controller;

import com.ssafy.modu.domain.notification.dto.response.NotificationResponse;
import com.ssafy.modu.domain.notification.service.NotificationService;
import com.ssafy.modu.global.auth.security.CustomUserDetails;
import com.ssafy.modu.global.common.ApiResponse;
import com.ssafy.modu.global.common.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<NotificationResponse> data =
                notificationService.getMyNotifications(userDetails.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        "알림 목록 조회에 성공했습니다.",
                        data
                )
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        long data = notificationService.countUnreadNotifications(userDetails.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        "읽지 않은 알림 개수 조회에 성공했습니다.",
                        data
                )
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(
                userDetails.getUserId(),
                notificationId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        "알림이 읽음 처리되었습니다."
                )
        );
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        notificationService.deleteNotification(
                userDetails.getUserId(),
                notificationId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        "알림이 삭제되었습니다."
                )
        );
    }
}