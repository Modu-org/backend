package com.ssafy.modu.domain.voicesearch.controller;

import com.ssafy.modu.domain.voicesearch.dto.request.VoiceSearchRequest;
import com.ssafy.modu.domain.voicesearch.service.VoiceSearchService;
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
@RequestMapping("/api/voice-search")
public class VoiceSearchController {

        private final VoiceSearchService voiceSearchService;

        /**
         * 음성 검색 API
         *
         * 프론트엔드에서 브라우저 음성 입력으로 받은 자연어 텍스트를
         * AI를 통해 구조화된 검색 파라미터로 변환하고,
         * type에 따라 적절한 도메인 API를 호출하여 결과를 반환한다.
         *
         * 인증 필수 아님
         * 인증된 사용자라면 userId를 사용하여 개인화된 접근성 설정을 적용한다.
         * 비인증 사용자라면 요청 파라미터만으로 검색한다.
         */
        @PostMapping
        public ResponseEntity<ApiResponse<Object>> search(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Valid @RequestBody VoiceSearchRequest request) {
                Long userId = userDetails != null ? userDetails.getUserId() : null;

                Object data = voiceSearchService.search(
                                request.getText(),
                                request.getType(),
                                userId);

                return ResponseEntity.ok(
                                ApiResponse.success(SuccessCode.OK, "음성 검색에 성공했습니다.", data));
        }
}
