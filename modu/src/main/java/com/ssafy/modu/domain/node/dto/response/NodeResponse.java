package com.ssafy.modu.domain.node.dto.response;

import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.node.entity.Node;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class NodeResponse {

    private Long nodeId;
    private Long scheduleId;
    private Long attractionId;
    private Integer visitOrder;
    private LocalDate visitDate;
    private Integer contentTypeId;
    private String placeName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;

    public static NodeResponse from(Node node) {
        Attraction attraction = node.getAttraction();

        return NodeResponse.builder()
                .nodeId(node.getId())
                .scheduleId(node.getSchedule().getId())
                .attractionId(attraction.getId())
                .placeName(attraction.getName())
                .address(attraction.getAddress())
                .latitude(attraction.getLatitude())
                .longitude(attraction.getLongitude())
                .contentTypeId(parseContentTypeId(attraction.getContentTypeId()))
                .visitOrder(node.getVisitOrder())
                .visitDate(node.getVisitDate())
                .build();
    }

    private static Integer parseContentTypeId(String contentTypeId) {
        if (contentTypeId == null || contentTypeId.isBlank()) {
            return 0;
        }
        return Integer.parseInt(contentTypeId);
    }
}
