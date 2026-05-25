package com.ssafy.modu.domain.region.controller;

import com.ssafy.modu.domain.region.dto.RegionResponse;
import com.ssafy.modu.domain.region.service.RegionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionService regionService;

    @GetMapping
    public List<RegionResponse> getRegions() {
        return regionService.getRegions();
    }

    @PostMapping("/sync")
    public String syncRegions() {
        regionService.syncRegions();
        return "법정동 코드 동기화 완료";
    }
}