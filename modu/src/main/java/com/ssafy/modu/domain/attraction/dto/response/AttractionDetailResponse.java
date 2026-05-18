package com.ssafy.modu.domain.attraction.dto.response;

import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.accessibility.entity.AccessibilityInfo;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Getter
@Builder
public class AttractionDetailResponse {

    private Long attractionId;
    private String contentId;
    private String name;
    private String address;
    private String addressDetail;
    private String zipcode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String contentTypeId;
    private String tel;
    private String firstImageUrl;
    private String thumbnailImageUrl;
    private String overview;
    private String homepage;
    private List<AttractionAccessibilityResponse> accessibility;

    public static AttractionDetailResponse from(Attraction attraction) {
        return AttractionDetailResponse.builder()
                .attractionId(attraction.getId())
                .contentId(attraction.getContentId())
                .name(attraction.getName())
                .address(attraction.getAddress())
                .addressDetail(attraction.getAddressDetail())
                .zipcode(attraction.getZipcode())
                .latitude(attraction.getLatitude())
                .longitude(attraction.getLongitude())
                .contentTypeId(attraction.getContentTypeId())
                .tel(attraction.getTel())
                .firstImageUrl(attraction.getFirstImageUrl())
                .thumbnailImageUrl(attraction.getThumbnailImageUrl())
                .overview(attraction.getOverview())
                .homepage(attraction.getHomepage())
                .accessibility(
                        attraction.getAccessibilityInfos().stream()
                                .sorted(
                                        Comparator
                                                .comparing((AccessibilityInfo info) -> info.getCategory().name())
                                                .thenComparing(info -> info.getType().name())
                                )
                                .map(AttractionAccessibilityResponse::from)
                                .toList()
                )
                .build();
    }
}