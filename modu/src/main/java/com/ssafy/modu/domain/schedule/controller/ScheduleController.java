package com.ssafy.modu.domain.schedule.controller;

import com.ssafy.modu.domain.schedule.dto.request.ScheduleArrivalShareRequest;
import com.ssafy.modu.domain.schedule.dto.request.ScheduleCreateRequest;
import com.ssafy.modu.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.ssafy.modu.domain.schedule.dto.response.*;
import com.ssafy.modu.domain.schedule.service.ScheduleService;
import com.ssafy.modu.global.auth.security.CustomUserDetails;
import com.ssafy.modu.global.common.ApiResponse;
import com.ssafy.modu.global.common.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    // 여행 스케줄 생성
    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ScheduleCreateRequest request
    ) {
        ScheduleResponse data = scheduleService.createSchedule(userDetails.getUserId(), request);
        return ResponseEntity.status(SuccessCode.CREATED.getHttpStatus())
                .body(ApiResponse.success(SuccessCode.CREATED, "여행 스케줄이 생성되었습니다.", data));
    }
    // 여행 스케줄 목록 보기
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleSummaryResponse>>> getSchedules(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<ScheduleSummaryResponse> data = scheduleService.getSchedules(userDetails.getUserId());

        if (data.isEmpty()) {
            return ResponseEntity.ok(
                    ApiResponse.success(SuccessCode.NO_CONTENT_DATA, "스케줄이 없습니다.", data)
            );
        }

        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.OK, "여행 목록 조회에 성공했습니다.", data)
        );
    }

    // 여행 상세 정보 조회
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleDetailResponse>> getScheduleDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long scheduleId
    ) {
        ScheduleDetailResponse data = scheduleService.getScheduleDetail(userDetails.getUserId(), scheduleId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, "여행 상세 조회에 성공했습니다.", data));
    }

    // 여행 스케줄 정보 수정
    @PostMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleUpdateRequest request
    ) {
        ScheduleResponse data = scheduleService.updateSchedule(userDetails.getUserId(), scheduleId, request);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, "여행 스케줄이 수정되었습니다.", data));
    }

    // 여행 스케줄 삭제
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long scheduleId
    ) {
        scheduleService.deleteSchedule(userDetails.getUserId(), scheduleId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, "여행 스케줄이 삭제되었습니다."));
    }

    // 여행 스케줄 요약 정보 보기
    @GetMapping("/{scheduleId}/summary")
    public ResponseEntity<ApiResponse<ScheduleSummaryResponse>> getScheduleSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long scheduleId
    ) {
        ScheduleSummaryResponse data = scheduleService.getScheduleSummary(userDetails.getUserId(), scheduleId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, "스케줄 요약 조회에 성공했습니다.", data));
    }

    @PatchMapping("/{scheduleId}/arrival-notification")
    public ResponseEntity<ApiResponse<Void>> updateArrivalShared(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleArrivalShareRequest request
    ) {
        scheduleService.updateArrivalShared(
                userDetails.getUserId(),
                scheduleId,
                request.getEnabled()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        "보호자 도착 알림 설정이 변경되었습니다."
                )
        );
    }
}
