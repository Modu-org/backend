package com.ssafy.modu.domain.voicesearch.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ParsedFiltersResponse {

    private String regionCode;
    private String sigunguCode;
    private String keyword;

    private Integer page;
    private Integer size;

    private Boolean physical;
    private Boolean infantFamily;

    @JsonProperty("infant_family")
    private Boolean infantFamilySnake;

    private Boolean visual;
    private Boolean hearing;

    private List<String> contentTypeIds;
    private String contentTypeId;

    public static ParsedFiltersResponse from(VoiceSearchParsedResult parsed) {
        if (parsed == null) {
            return null;
        }

        String firstContentTypeId = null;
        if (parsed.getContentTypeIds() != null && !parsed.getContentTypeIds().isEmpty()) {
            firstContentTypeId = parsed.getContentTypeIds().get(0);
        }

        return ParsedFiltersResponse.builder()
                .regionCode(parsed.getRegionCode())
                .sigunguCode(parsed.getSigunguCode())
                .keyword(parsed.getKeyword())
                .page(parsed.getPage())
                .size(parsed.getSize())
                .physical(parsed.getPhysical())
                .infantFamily(parsed.getInfantFamily())
                .infantFamilySnake(parsed.getInfantFamily())
                .visual(parsed.getVisual())
                .hearing(parsed.getHearing())
                .contentTypeIds(parsed.getContentTypeIds())
                .contentTypeId(firstContentTypeId)
                .build();
    }
}
