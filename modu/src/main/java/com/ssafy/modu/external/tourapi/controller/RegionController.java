package com.ssafy.modu.external.tourapi.controller;

import com.ssafy.modu.external.tourapi.TourApiRegionService;
import com.ssafy.modu.external.tourapi.dto.RegionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/regions")
public class RegionController {

    private final TourApiRegionService regionService;

    @GetMapping
    public List<RegionResponse> getRegions() {
        return regionService.getRegionsWithDistricts();
    }
}