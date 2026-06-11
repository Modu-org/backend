package com.ssafy.modu.domain.attraction.ranking.dto;

import com.ssafy.modu.domain.attraction.entity.Attraction;

public record PopularAttractionResponse(
        Long attractionId,
        String name,
        String address,
        String thumbnailImageUrl,
        String contentTypeId,
        long addedCount
) {

    public static PopularAttractionResponse from(Attraction attraction, long addedCount) {
        return new PopularAttractionResponse(
                attraction.getId(),
                attraction.getName(),
                attraction.getAddress(),
                attraction.getThumbnailImageUrl(),
                attraction.getContentTypeId(),
                addedCount
        );
    }
}
