package com.ssafy.modu.domain.region.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.modu.domain.region.dto.DistrictResponse;
import com.ssafy.modu.domain.region.dto.RegionResponse;
import com.ssafy.modu.domain.region.entity.LegalDongCode;
import com.ssafy.modu.domain.region.repository.LegalDongCodeRepository;
import com.ssafy.modu.external.tourapi.TourApiService;
import com.ssafy.modu.external.tourapi.util.TourApiJsonExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final TourApiService tourApiService;
    private final TourApiJsonExtractor extractor;
    private final LegalDongCodeRepository legalDongCodeRepository;

    @Transactional
    public void syncRegions() {
        JsonNode root = tourApiService.getAllLDongCodes();

        for (JsonNode item : extractor.items(root)) {
            String regionCode = extractor.text(item, "lDongRegnCd");
            String regionName = extractor.text(item, "lDongRegnNm");
            String districtCode = extractor.text(item, "lDongSignguCd");
            String districtName = extractor.text(item, "lDongSignguNm");

            if (regionCode == null || regionName == null ||
                    districtCode == null || districtName == null) {
                continue;
            }

            legalDongCodeRepository
                    .findByRegionCodeAndDistrictCode(regionCode, districtCode)
                    .ifPresentOrElse(
                            code -> code.update(regionName, districtName),
                            () -> legalDongCodeRepository.save(
                                    new LegalDongCode(
                                            regionCode,
                                            regionName,
                                            districtCode,
                                            districtName
                                    )
                            )
                    );
        }
    }

    @Transactional(readOnly = true)
    public List<RegionResponse> getRegions() {
        List<LegalDongCode> codes =
                legalDongCodeRepository.findAllByOrderByRegionCodeAscDistrictCodeAsc();

        Map<String, RegionGroup> regionMap = new LinkedHashMap<>();

        for (LegalDongCode code : codes) {
            RegionGroup group = regionMap.computeIfAbsent(
                    code.getRegionCode(),
                    regionCode -> new RegionGroup(
                            code.getRegionCode(),
                            code.getRegionName(),
                            new ArrayList<>()
                    )
            );

            group.districts().add(
                    new DistrictResponse(
                            code.getDistrictCode(),
                            code.getDistrictName()
                    )
            );
        }

        return regionMap.values().stream()
                .map(group -> new RegionResponse(
                        group.regionCode(),
                        group.regionName(),
                        group.districts()
                ))
                .toList();
    }

    private record RegionGroup(
            String regionCode,
            String regionName,
            List<DistrictResponse> districts
    ) {
    }
}