package com.ssafy.modu.domain.schedule.dto.response;

import com.ssafy.modu.domain.node.dto.response.NodeResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
// 특정 날짜(date)에 배치된 노드 목록(nodes)을 담는다.
@Getter
@Builder
public class ScheduleDayResponse {

    private LocalDate date;
    private List<NodeResponse> nodes;
}
