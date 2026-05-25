package com.ssafy.modu.external.tourapi;

import com.ssafy.modu.external.tourapi.dto.DistrictResponse;
import com.ssafy.modu.external.tourapi.dto.RegionResponse;
import com.ssafy.modu.external.tourapi.util.TourApiJsonExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TourApiRegionService {

    private final TourApiService tourApiService;
    private final TourApiJsonExtractor extractor;

    public List<RegionResponse> getRegionsWithDistricts() {
        JsonNode root = tourApiService.getAllLDongCodes();

        Map<String, RegionGroup> regionMap = new LinkedHashMap<>();

        // 시도 - 시군구 코드 전부다 가져와서 순회
        for (JsonNode item : extractor.items(root)) {
            String regionCode = extractor.text(item, "lDongRegnCd");
            String regionName = extractor.text(item, "lDongRegnNm");
            String districtCode = extractor.text(item, "lDongSignguCd");
            String districtName = extractor.text(item, "lDongSignguNm");

            if (regionCode == null || regionName == null) {
                continue;
            }
            // 각 도시마다 {코드, RegionGroup} 쌍 만듦
            RegionGroup group = regionMap.computeIfAbsent(
                    regionCode,
                    code -> new RegionGroup(regionCode, regionName, new ArrayList<>())
            );

            if (districtCode != null && districtName != null) {
                group.districts().add(new DistrictResponse(districtCode, districtName));
            }
        }
        // 만들어진 regionMap을 그대로 dto로 변환
        return regionMap.values().stream()
                .map(group -> new RegionResponse(
                        group.regionCode(),
                        group.regionName(),
                        group.districts()
                ))
                .toList();
    }

    private record RegionGroup(
            // 시도코드
            String regionCode,
            // 시도이름
            String regionName,
            // {시군구 코드, 시군구 이름} 리스트
            List<DistrictResponse> districts
    ) {
    }
}