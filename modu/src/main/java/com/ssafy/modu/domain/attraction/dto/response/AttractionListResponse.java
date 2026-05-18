package com.ssafy.modu.domain.attraction.dto.response;

import com.ssafy.modu.domain.attraction.entity.Attraction;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

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

    public static AttractionListResponse from(Attraction attraction) {
        return AttractionListResponse.builder()
                .attractionId(attraction.getId())
                .contentId(attraction.getContentId())
                .name(attraction.getName())
                .address(attraction.getAddress())
                .thumbnailImageUrl(attraction.getThumbnailImageUrl())
                .contentTypeId(attraction.getContentTypeId())
                .latitude(attraction.getLatitude())
                .longitude(attraction.getLongitude())
                .build();
    }
}