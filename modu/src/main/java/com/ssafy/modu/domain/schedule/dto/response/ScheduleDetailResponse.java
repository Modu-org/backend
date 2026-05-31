package com.ssafy.modu.domain.schedule.dto.response;

import com.ssafy.modu.domain.edge.dto.response.EdgeResponse;
import com.ssafy.modu.domain.edge.entity.Edge;
import com.ssafy.modu.domain.node.dto.response.NodeResponse;
import com.ssafy.modu.domain.node.entity.Node;
import com.ssafy.modu.domain.schedule.entity.Schedule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        return from(schedule, List.of());
    }

    public static ScheduleDetailResponse from(Schedule schedule, List<Edge> edges) {
        Map<LocalDate, List<Node>> grouped = schedule.getNodes().stream()
                .filter(node -> node.getVisitDate() != null)
                .collect(Collectors.groupingBy(
                        Node::getVisitDate,
                        TreeMap::new,
                        Collectors.toList()
                ));

        Map<String, Edge> edgeMap = edges.stream()
                .collect(Collectors.toMap(
                        edge -> edgeKey(edge.getFromNode().getId(), edge.getToNode().getId()),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        List<ScheduleDayResponse> days = new ArrayList<>();

        LocalDate current = schedule.getStartDate();
        while (!current.isAfter(schedule.getEndDate())) {
            List<Node> sortedNodes = grouped.getOrDefault(current, List.of()).stream()
                    .sorted(Comparator.comparing(Node::getVisitOrder, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(Node::getId))
                    .toList();

            List<NodeResponse> nodes = sortedNodes.stream()
                    .map(NodeResponse::from)
                    .toList();

            List<EdgeResponse> activeEdges = buildActiveEdges(sortedNodes, edgeMap);

            days.add(ScheduleDayResponse.builder()
                    .date(current)
                    .nodes(nodes)
                    .edges(activeEdges)
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

    private static List<EdgeResponse> buildActiveEdges(
            List<Node> sortedNodes,
            Map<String, Edge> edgeMap
    ) {
        List<EdgeResponse> responses = new ArrayList<>();

        for (int i = 0; i < sortedNodes.size() - 1; i++) {
            Node fromNode = sortedNodes.get(i);
            Node toNode = sortedNodes.get(i + 1);

            Edge edge = edgeMap.get(edgeKey(fromNode.getId(), toNode.getId()));

            if (edge != null) {
                responses.add(EdgeResponse.from(edge));
            }
        }

        return responses;
    }

    private static String edgeKey(Long fromNodeId, Long toNodeId) {
        return fromNodeId + "-" + toNodeId;
    }
}