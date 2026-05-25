package com.ssafy.modu.domain.region.dto;

import java.util.List;

public record RegionResponse(
        String regionCode,
        String regionName,
        List<DistrictResponse> districts
) {
}