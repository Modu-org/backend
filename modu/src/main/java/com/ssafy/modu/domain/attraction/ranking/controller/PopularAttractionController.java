package com.ssafy.modu.domain.attraction.ranking.controller;

import com.ssafy.modu.domain.attraction.ranking.dto.PopularAttractionResponse;
import com.ssafy.modu.domain.attraction.ranking.service.PopularAttractionQueryService;
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
    public ResponseEntity<List<PopularAttractionResponse>> getPopularAttractions(
            @PathVariable String regionCode,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(
                popularAttractionQueryService.getPopularAttractions(regionCode, limit)
        );
    }
}
