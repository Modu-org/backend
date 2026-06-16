package com.ssafy.modu.domain.aicommand.controller;

import com.ssafy.modu.domain.aicommand.dto.request.AiScheduleCommandRequest;
import com.ssafy.modu.domain.aicommand.dto.response.AiScheduleCommandResponse;
import com.ssafy.modu.domain.aicommand.service.AiScheduleCommandService;
import com.ssafy.modu.global.auth.security.CustomUserDetails;
import com.ssafy.modu.global.common.ApiResponse;
import com.ssafy.modu.global.common.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/schedule-workflow")
public class AiScheduleWorkflowCommandController {
    private final AiScheduleCommandService aiScheduleCommandService;
    @PostMapping
    public ResponseEntity<ApiResponse<AiScheduleCommandResponse>> handleWorkflowCommand(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AiScheduleCommandRequest request
    ) {
        AiScheduleCommandResponse data = aiScheduleCommandService.handleScheduleWorkflowCommand(
                userDetails.getUserId(),
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.OK, "AI 일정 명령 처리에 성공했습니다.", data)
        );
    }
}
