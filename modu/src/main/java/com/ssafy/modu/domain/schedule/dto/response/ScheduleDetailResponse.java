package com.ssafy.modu.domain.schedule.dto.response;

import com.ssafy.modu.domain.node.dto.response.NodeResponse;
import com.ssafy.modu.domain.node.entity.Node;
import com.ssafy.modu.domain.schedule.entity.Schedule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 스케줄 기본 정보와 날짜별 노드 목록을 함께 반환한다.
 * days에는 여행 시작일부터 종료일까지의 날짜별 노드 목록이 들어간다.
 * unscheduledNodes에는 아직 방문일자가 지정되지 않은 노드들이 들어간다.
 */
@Getter
@Builder
public class ScheduleDetailResponse {

    private Long scheduleId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<ScheduleDayResponse> days;
    private List<NodeResponse> unscheduledNodes;

    public static ScheduleDetailResponse from(Schedule schedule) {
        Map<LocalDate, List<Node>> grouped = schedule.getNodes().stream()
                .filter(node -> node.getVisitDate() != null)
                .collect(Collectors.groupingBy(Node::getVisitDate, TreeMap::new, Collectors.toList()));

        List<ScheduleDayResponse> days = new ArrayList<>();
        LocalDate current = schedule.getStartDate();
        while (!current.isAfter(schedule.getEndDate())) {
            List<NodeResponse> nodes = grouped.getOrDefault(current, List.of()).stream()
                    .sorted(Comparator.comparing(Node::getVisitOrder, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(Node::getId))
                    .map(NodeResponse::from)
                    .toList();

            days.add(ScheduleDayResponse.builder()
                    .date(current)
                    .nodes(nodes)
                    .build());

            current = current.plusDays(1);
        }

        List<NodeResponse> unscheduledNodes = schedule.getNodes().stream()
                .filter(node -> node.getVisitDate() == null)
                .sorted(Comparator.comparing(Node::getId))
                .map(NodeResponse::from)
                .toList();

        return ScheduleDetailResponse.builder()
                .scheduleId(schedule.getId())
                .title(schedule.getTitle())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .days(days)
                .unscheduledNodes(unscheduledNodes)
                .build();
    }
}
