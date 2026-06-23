package com.ssafy.modu.domain.arrival.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NextDestinationResponse {

    private Long nodeId;
    private Long attractionId;
    private String placeName;
    private String address;
    private Double latitude;
    private Double longitude;
    private Integer distanceMeters;
    private Integer estimatedTimeMinutes;

    public static NextDestinationResponse of(
            Long nodeId,
            Long attractionId,
            String placeName,
            String address,
            Double latitude,
            Double longitude,
            Integer distanceMeters,
            Integer estimatedTimeMinutes
    ) {
        return NextDestinationResponse.builder()
                .nodeId(nodeId)
                .attractionId(attractionId)
                .placeName(placeName)
                .address(address)
                .latitude(latitude)
                .longitude(longitude)
                .distanceMeters(distanceMeters)
                .estimatedTimeMinutes(estimatedTimeMinutes)
                .build();
    }
}