package com.ssafy.modu.domain.node.dto.response;

import com.ssafy.modu.domain.attraction.dto.response.AttractionAccessibilityResponse;
import com.ssafy.modu.domain.attraction.entity.Attraction;
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
    private Integer contentTypeId;
    private String placeName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private List<AttractionAccessibilityResponse> accessibility;

    public static NodeDetailResponse from(Node node) {
        Attraction attraction = node.getAttraction();

        return NodeDetailResponse.builder()
                .nodeId(node.getId())
                .scheduleId(node.getSchedule().getId())
                .attractionId(attraction.getId())
                .contentTypeId(parseContentTypeId(attraction.getContentTypeId()))
                .placeName(attraction.getName())
                .address(attraction.getAddress())
                .latitude(attraction.getLatitude())
                .longitude(attraction.getLongitude())
                .visitOrder(node.getVisitOrder())
                .visitDate(node.getVisitDate())
                .accessibility(
                        attraction.getAccessibilityInfos().stream()
                                .sorted(
                                        Comparator.comparingInt(AccessibilityInfo::getSourcePriority)
                                                .thenComparing(info -> info.getCategory().getCode())
                                                .thenComparing(info -> info.getType().getCode())
                                )
                                .map(AttractionAccessibilityResponse::from)
                                .toList()
                )
                .build();
    }

    private static Integer parseContentTypeId(String contentTypeId) {
        if (contentTypeId == null || contentTypeId.isBlank()) {
            return 0;
        }
        return Integer.parseInt(contentTypeId);
    }
}
