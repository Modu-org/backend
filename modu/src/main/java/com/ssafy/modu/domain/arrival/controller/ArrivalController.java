package com.ssafy.modu.domain.arrival.controller;

import com.ssafy.modu.domain.arrival.dto.request.ArrivalRequest;
import com.ssafy.modu.domain.arrival.dto.response.ArrivalLogDetailResponse;
import com.ssafy.modu.domain.arrival.dto.response.ArrivalResponse;
import com.ssafy.modu.domain.arrival.service.ArrivalService;
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
@RequestMapping("/api")
public class ArrivalController {

    private final ArrivalService arrivalService;

    @PostMapping("/schedules/{scheduleId}/nodes/{nodeId}/arrival")
    public ResponseEntity<ApiResponse<ArrivalResponse>> confirmArrival(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long scheduleId,
            @PathVariable Long nodeId,
            @Valid @RequestBody ArrivalRequest request
    ) {
        ArrivalResponse response = arrivalService.confirmArrival(
                userDetails.getUserId(),
                scheduleId,
                nodeId,
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        response.getMessage(),
                        response
                )
        );
    }
    @GetMapping("/arrival-logs/{arrivalLogId}")
    public ResponseEntity<ApiResponse<ArrivalLogDetailResponse>> getArrivalLogDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long arrivalLogId
    ) {
        ArrivalLogDetailResponse data = arrivalService.getArrivalLogDetail(
                userDetails.getUserId(),
                arrivalLogId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        "도착 알림 상세 조회에 성공했습니다.",
                        data
                )
        );
    }
}