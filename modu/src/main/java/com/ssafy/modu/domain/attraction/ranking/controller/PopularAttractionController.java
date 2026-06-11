package com.ssafy.modu.domain.attraction.ranking.controller;

import com.ssafy.modu.domain.attraction.ranking.dto.PopularAttractionResponse;
import com.ssafy.modu.domain.attraction.ranking.service.PopularAttractionQueryService;
import com.ssafy.modu.global.common.ApiResponse;
import com.ssafy.modu.global.common.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/regions")
public class PopularAttractionController {

    private final PopularAttractionQueryService popularAttractionQueryService;

    @GetMapping("/{regionCode}/popular-attractions")
    public ResponseEntity<ApiResponse<List<PopularAttractionResponse>>> getPopularAttractions(
            @PathVariable String regionCode,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<PopularAttractionResponse> response =
                popularAttractionQueryService.getPopularAttractions(regionCode, limit);

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        "지역별 인기 관광지 목록 조회에 성공했습니다.",
                        response
                )
        );
    }
}
