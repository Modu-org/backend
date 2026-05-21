package com.ssafy.modu.domain.voicesearch.service;

import com.ssafy.modu.domain.attraction.dto.request.AttractionSearchRequest;
import com.ssafy.modu.domain.attraction.dto.response.AttractionListResponse;
import com.ssafy.modu.domain.attraction.dto.response.AttractionPageResponse;
import com.ssafy.modu.domain.attraction.service.AttractionService;
import com.ssafy.modu.domain.voicesearch.dto.response.VoiceSearchParsedResult;
import com.ssafy.modu.external.ai.AIService;
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

    /**
     * 음성 검색을 처리한다.
     *
     * 역할:
     * 1. AIService를 통해 자연어 텍스트를 구조화된 파라미터로 변환한다.
     * 2. type에 따라 적절한 도메인 서비스를 호출한다.
     * 3. 결과를 반환한다.
     *
     * type 1: 관광지 검색
     * type 2, 3: 향후 확장 예정
     */
    public Object search(String text, int type, Long userId) {
        return switch (type) {
            case 1 -> searchAttractions(text, userId);
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
     * 4. 결과를 AttractionPageResponse로 변환하여 반환한다.
     */
    private AttractionPageResponse searchAttractions(String text, Long userId) {
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

        return AttractionPageResponse.from(page);
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
}
