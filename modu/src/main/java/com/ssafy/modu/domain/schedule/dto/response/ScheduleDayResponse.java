package com.ssafy.modu.domain.schedule.dto.response;

import com.ssafy.modu.domain.edge.dto.response.EdgeResponse;
import com.ssafy.modu.domain.node.dto.response.NodeResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ScheduleDayResponse {

    private LocalDate date;
    private List<NodeResponse> nodes;

    // 현재 visitOrder 기준으로 화면에 실제 표시할 간선
    private List<EdgeResponse> edges;
}