package com.ssafy.modu.domain.node.dto.response;

import com.ssafy.modu.domain.attraction.dto.response.AttractionAccessibilityResponse;
import com.ssafy.modu.domain.node.entity.Node;
import com.ssafy.modu.domain.accessibility.entity.AccessibilityInfo;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Getter
@Builder
public class NodeDetailResponse {

    private Long nodeId;
    private Long scheduleId;
    private Integer visitOrder;
    private LocalDate visitDate;
    private Long attractionId;
    private String placeName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private List<AttractionAccessibilityResponse> accessibility;

    public static NodeDetailResponse from(Node node) {
        return NodeDetailResponse.builder()
                .nodeId(node.getId())
                .scheduleId(node.getSchedule().getId())
                .visitOrder(node.getVisitOrder())
                .visitDate(node.getVisitDate())
                .attractionId(node.getAttraction().getId())
                .placeName(node.getPlaceName())
                .address(node.getAddress())
                .latitude(node.getLatitude())
                .longitude(node.getLongitude())
                .accessibility(node.getAttraction().getAccessibilityInfos().stream()
                        .sorted(Comparator
                                .comparing((AccessibilityInfo info) -> info.getCategory().name())
                                .thenComparing(info -> info.getType().name()))
                        .map(AttractionAccessibilityResponse::from)
                        .toList())
                .build();
    }
}
