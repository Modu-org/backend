package com.ssafy.modu.domain.arrival.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NextDestinationResponse {

    private Long nodeId;
    private String placeName;
    private String address;
    private Integer distanceMeters;
    private Integer estimatedTimeMinutes;

    public static NextDestinationResponse of(
            Long nodeId,
            String placeName,
            String address,
            Integer distanceMeters,
            Integer estimatedTimeMinutes
    ) {
        return NextDestinationResponse.builder()
                .nodeId(nodeId)
                .placeName(placeName)
                .address(address)
                .distanceMeters(distanceMeters)
                .estimatedTimeMinutes(estimatedTimeMinutes)
                .build();
    }
}