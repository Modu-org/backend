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
        List<PopularAttractionResponse> data =
                popularAttractionQueryService.getPopularAttractions(regionCode, limit);

        if (data.isEmpty()) {
            return ResponseEntity.ok(
                    ApiResponse.success(
                            SuccessCode.NO_CONTENT_DATA,
                            "해당 지역의 인기 관광지 데이터가 없습니다.",
                            data
                    )
            );
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessCode.OK,
                        "지역별 인기 관광지 목록 조회에 성공했습니다.",
                        data
                )
        );
    }
}
