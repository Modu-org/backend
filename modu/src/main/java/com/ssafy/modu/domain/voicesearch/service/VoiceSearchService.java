package com.ssafy.modu.domain.voicesearch.service;

import com.ssafy.modu.domain.attraction.dto.request.AttractionSearchRequest;
import com.ssafy.modu.domain.attraction.dto.response.AttractionAccessibilityResponse;
import com.ssafy.modu.domain.attraction.dto.response.AttractionDetailResponse;
import com.ssafy.modu.domain.attraction.dto.response.AttractionListResponse;
import com.ssafy.modu.domain.attraction.dto.response.AttractionPageResponse;
import com.ssafy.modu.domain.attraction.service.AttractionService;
import com.ssafy.modu.domain.user.entity.UserDetail;
import com.ssafy.modu.domain.user.repository.UserDetailRepository;
import com.ssafy.modu.domain.voicesearch.dto.response.VoiceSearchDetailResponse;
import com.ssafy.modu.domain.voicesearch.dto.response.VoiceSearchParsedResult;
import com.ssafy.modu.domain.voicesearch.dto.response.VoiceSearchResponse;
import com.ssafy.modu.external.ai.AIService;
import com.ssafy.modu.external.ai.AIPromptTemplate;
import com.ssafy.modu.external.ai.client.GmsGeminiClient;
import com.ssafy.modu.external.ai.client.GmsOpenAIClient;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceSearchService {

    private final AIService aiService;
    private final AttractionService attractionService;
    private final UserDetailRepository userDetailRepository;
    private final GmsGeminiClient gmsGeminiClient;
    private final GmsOpenAIClient gmsOpenAIClient;

    /**
     * 음성 검색 및 기능 처리를 수행한다.
     *
     * 역할:
     * 1. type에 따라 적절한 서비스를 호출한다.
     * 2. 결과를 반환한다.
     *
     * type 1: 관광지 검색
     * type 2: 관광지 상세 페이지 정보 음성 안내
     */
    public Object search(String text, int type, Long userId, Long attractionId) {
        return switch (type) {
            case 1 -> searchAttractions(text, userId);
            case 2 -> readAttractionDetail(text, userId, attractionId);
            default -> throw new BusinessException(ErrorCode.INVALID_VOICE_SEARCH_TYPE);
        };
    }

    /**
     * type 1: 관광지 검색.
     *
     * 역할:
     * 1. Gemini API로 텍스트를 파싱하여 검색 파라미터를 얻는다.
     * 2. 파싱 결과를 AttractionSearchRequest로 변환한다.
     * 3. AttractionService.searchAttractions()를 호출한다.
     * 4. 결과를 VoiceSearchResponse(관광지 목록 + 파싱된 필터)로 변환하여 반환한다.
     */
    private VoiceSearchResponse searchAttractions(String text, Long userId) {
        /*
         * Gemini API를 호출하여 자연어를 구조화된 파라미터로 변환한다.
         */
        VoiceSearchParsedResult parsed = aiService.parseVoiceInput(text, 1);

        log.info(
                "음성 검색 파싱 결과: regionCode={}, sigunguCode={}, keyword={}, physical={}, visual={}, hearing={}, infantFamily={}, contentTypeIds={}",
                parsed.getRegionCode(), parsed.getSigunguCode(), parsed.getKeyword(),
                parsed.getPhysical(), parsed.getVisual(), parsed.getHearing(),
                parsed.getInfantFamily(), parsed.getContentTypeIds());

        /*
         * 파싱 결과를 기존 AttractionSearchRequest로 변환한다.
         * 기존 관광지 조회 API와 동일한 요청 객체를 사용하여 일관성을 유지한다.
         */
        AttractionSearchRequest request = toAttractionSearchRequest(parsed);

        /*
         * 기존 AttractionService를 호출하여 관광지를 조회한다.
         * userId가 null인 경우(비인증 사용자)에는 접근성 기본값 없이 요청 파라미터만 사용한다.
         */
        Page<AttractionListResponse> page = attractionService.searchAttractions(userId, request);
        AttractionPageResponse attractionPage = AttractionPageResponse.from(page);

        return VoiceSearchResponse.of(attractionPage, parsed);
    }

    /**
     * VoiceSearchParsedResult를 AttractionSearchRequest로 변환한다.
     *
     * Gemini가 반환한 값을 그대로 AttractionSearchRequest 필드에 매핑한다.
     */
    private AttractionSearchRequest toAttractionSearchRequest(VoiceSearchParsedResult parsed) {
        AttractionSearchRequest request = new AttractionSearchRequest();
        request.setRegionCode(parsed.getRegionCode());
        request.setSigunguCode(parsed.getSigunguCode());
        request.setKeyword(parsed.getKeyword());
        request.setPage(parsed.getPage() != null ? parsed.getPage() : 0);
        request.setSize(parsed.getSize() != null ? parsed.getSize() : 20);
        request.setPhysical(parsed.getPhysical());
        request.setInfantFamily(parsed.getInfantFamily());
        request.setVisual(parsed.getVisual());
        request.setHearing(parsed.getHearing());
        request.setContentTypeIds(parsed.getContentTypeIds());
        return request;
    }

    /**
     * type 2: 관광지 상세 페이지 정보 음성 안내.
     */
    private VoiceSearchDetailResponse readAttractionDetail(String text, Long userId, Long attractionId) {
        if (attractionId == null) {
            throw new BusinessException(ErrorCode.ATTRACTION_NOT_FOUND);
        }

        // 1. 관광지 정보 조회
        AttractionDetailResponse attraction = attractionService.getAttractionDetail(attractionId);

        // 2. 유저 정보 조회 (비로그인인 경우 모든 설정 false)
        boolean physical = false;
        boolean infantFamily = false;
        boolean visual = false;
        boolean hearing = false;

        if (userId != null) {
            UserDetail userDetail = userDetailRepository.findById(userId).orElse(null);
            if (userDetail != null) {
                physical = Boolean.TRUE.equals(userDetail.getPhysical());
                infantFamily = Boolean.TRUE.equals(userDetail.getInfantFamily());
                visual = Boolean.TRUE.equals(userDetail.getVisual());
                hearing = Boolean.TRUE.equals(userDetail.getHearing());
            }
        }

        // 3. 접근성 정보 문자열 포맷팅
        StringBuilder accBuilder = new StringBuilder();
        if (attraction.getAccessibility() != null) {
            for (AttractionAccessibilityResponse acc : attraction.getAccessibility()) {
                accBuilder.append(String.format("- %s (%s): %s (설명: %s)\n",
                        acc.getCategory().name(),
                        acc.getType().name(),
                        acc.getStatus().name(),
                        acc.getDescription() != null ? acc.getDescription() : "없음"
                ));
            }
        }
        String accessibilityText = accBuilder.length() > 0 ? accBuilder.toString() : "제공된 접근성 편의시설 정보가 없습니다.";

        // 4. 프롬프트 채우기
        String prompt = String.format(
                AIPromptTemplate.ATTRACTION_DETAIL_READ,
                attraction.getName(),
                attraction.getAddress(),
                accessibilityText,
                attraction.getOverview() != null ? attraction.getOverview() : "상세 설명이 없습니다.",
                physical,
                infantFamily,
                visual,
                hearing,
                text
        );

        // 5. Gemini API 호출
        String responseText = gmsGeminiClient.generateContent(prompt);

        log.info("Gemini 음성 상세 가이드 응답: {}", responseText);

        // 6. 결과 파싱
        VoiceSearchDetailResponse detailResponse = aiService.parseVoiceDetailRead(responseText);

        // 7. TTS 음성 합성
        String audioDataBase64 = null;
        if (detailResponse.getReadText() != null && !detailResponse.getReadText().isBlank()) {
            try {
                byte[] speechBytes = gmsOpenAIClient.generateSpeech(detailResponse.getReadText());
                if (speechBytes != null && speechBytes.length > 0) {
                    audioDataBase64 = java.util.Base64.getEncoder().encodeToString(speechBytes);
                }
            } catch (Exception e) {
                log.error("음성 합성(TTS) 중 오류 발생: {}", e.getMessage(), e);
                // 에러 발생 시 audioData는 null로 두고 본문만 전송하여 500 에러를 방지함
            }
        }

        return VoiceSearchDetailResponse.builder()
                .readText(detailResponse.getReadText())
                .audioData(audioDataBase64)
                .build();
    }
}
