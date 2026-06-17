package com.ssafy.modu.domain.aicommand.controller;

import com.ssafy.modu.domain.aicommand.dto.request.VoiceCommandRequest;
import com.ssafy.modu.domain.aicommand.dto.response.VoiceCommandResponse;
import com.ssafy.modu.domain.aicommand.service.VoiceCommandRouter;
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
@RequestMapping("/api/ai/voice-command")
public class VoiceCommandController {

    private final VoiceCommandRouter voiceCommandRouter;

    @PostMapping
    public ResponseEntity<ApiResponse<VoiceCommandResponse>> handle(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VoiceCommandRequest request
    ) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;

        VoiceCommandResponse data = voiceCommandRouter.handle(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessCode.OK, "음성 명령 처리에 성공했습니다.", data)
        );
    }
}
