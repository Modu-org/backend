package com.ssafy.modu.domain.routerecommend.controller;

import com.ssafy.modu.domain.routerecommend.dto.request.AutoArrangeRequest;
import com.ssafy.modu.domain.routerecommend.service.RouteRecommendService;
import com.ssafy.modu.domain.schedule.dto.response.ScheduleDetailResponse;
import com.ssafy.modu.global.auth.security.CustomUserDetails;
import com.ssafy.modu.global.common.ApiResponse;
import com.ssafy.modu.global.common.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class RouteRecommendController {

    private final RouteRecommendService routeRecommendService;

    @PostMapping("/{scheduleId}/auto-arrange")
    public ResponseEntity<ApiResponse<ScheduleDetailResponse>> autoArrange(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long scheduleId,
            @RequestBody AutoArrangeRequest request
    ) {
        ScheduleDetailResponse data = routeRecommendService.autoArrange(
                scheduleId,
                userDetails.getUserId(),
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.OK, "경로 자동 정렬에 성공했습니다.", data)
        );
    }
}