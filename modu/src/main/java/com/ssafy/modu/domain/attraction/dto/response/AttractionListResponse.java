package com.ssafy.modu.domain.attraction.dto.response;

import com.ssafy.modu.domain.attraction.entity.Attraction;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class AttractionListResponse {

    private Long attractionId;
    private String contentId;
    private String name;
    private String address;
    private String thumbnailImageUrl;
    private String contentTypeId;
    private BigDecimal latitude;
    private BigDecimal longitude;

    private List<AttractionAccessibilityResponse> accessibility;

    public static AttractionListResponse from(
            Attraction attraction,
            List<AttractionAccessibilityResponse> accessibility
    ) {
        return AttractionListResponse.builder()
                .attractionId(attraction.getId())
                .contentId(attraction.getContentId())
                .name(attraction.getName())
                .address(attraction.getAddress())
                .thumbnailImageUrl(attraction.getThumbnailImageUrl())
                .contentTypeId(attraction.getContentTypeId())
                .latitude(attraction.getLatitude())
                .longitude(attraction.getLongitude())
                .accessibility(accessibility)
                .build();
    }

    public static AttractionListResponse from(Attraction attraction) {
        return from(attraction, List.of());
    }
}