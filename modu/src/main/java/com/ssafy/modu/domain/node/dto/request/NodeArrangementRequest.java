package com.ssafy.modu.domain.node.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class NodeArrangementRequest {

    @NotEmpty(message = "일자별 노드 배치 정보는 비어 있을 수 없습니다.")
    @Valid
    private List<DayArrangement> days;

    @Getter
    @NoArgsConstructor
    public static class DayArrangement {

        private LocalDate date; // null이면 미지정 그룹

        @Valid
        private List<NodeArrangement> nodes;
    }

    @Getter
    @NoArgsConstructor
    public static class NodeArrangement {

        @NotNull(message = "노드 ID는 필수입니다.")
        private Long nodeId;

        @Min(value = 1, message = "방문 순서는 1 이상이어야 합니다.")
        private Integer visitOrder;
    }
}