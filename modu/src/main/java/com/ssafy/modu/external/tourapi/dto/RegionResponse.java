package com.ssafy.modu.external.tourapi.dto;

import java.util.List;

public record RegionResponse(
        String regionCode,
        String regionName,
        List<DistrictResponse> districts
) {
}