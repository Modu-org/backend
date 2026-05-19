package com.ssafy.modu.domain.node.dto.response;

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
        return NodeResponse.builder()
                .nodeId(node.getId())
                .scheduleId(node.getSchedule().getId())
                .attractionId(node.getAttraction().getId())
                .visitOrder(node.getVisitOrder())
                .visitDate(node.getVisitDate())
                .contentTypeId(node.getContentTypeId())
                .placeName(node.getPlaceName())
                .address(node.getAddress())
                .latitude(node.getLatitude())
                .longitude(node.getLongitude())
                .build();
    }
}
