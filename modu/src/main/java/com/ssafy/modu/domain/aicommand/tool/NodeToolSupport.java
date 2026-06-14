package com.ssafy.modu.domain.aicommand.tool;

import com.ssafy.modu.domain.node.entity.Node;
import com.ssafy.modu.domain.node.repository.NodeRepository;
import com.ssafy.modu.domain.schedule.entity.Schedule;
import com.ssafy.modu.domain.schedule.repository.ScheduleRepository;
import com.ssafy.modu.global.exception.BusinessException;
import com.ssafy.modu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NodeToolSupport {

    private final ScheduleRepository scheduleRepository;
    private final NodeRepository nodeRepository;

    public Schedule getOwnedSchedule(Long userId, Long scheduleId) {
        return scheduleRepository.findByIdAndUser_Id(scheduleId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    public void validateDateInSchedule(Schedule schedule, LocalDate date) {
        if (date == null || date.isBefore(schedule.getStartDate()) || date.isAfter(schedule.getEndDate())) {
            throw new BusinessException(ErrorCode.INVALID_NODE_VISIT_DATE);
        }
    }

    public List<Node> getDayNodes(Long userId, Long scheduleId, LocalDate date) {
        Schedule schedule = getOwnedSchedule(userId, scheduleId);
        validateDateInSchedule(schedule, date);

        return nodeRepository.findWithAttractionBySchedule_IdAndVisitDate(scheduleId, date).stream()
                .sorted(Comparator.comparing(Node::getVisitOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Node::getId))
                .toList();
    }

    public Map<String, Object> toNodeMap(Node node) {
        return Map.of(
                "nodeId", node.getId(),
                "visitOrder", node.getVisitOrder(),
                "attractionId", node.getAttraction().getId(),
                "name", node.getAttraction().getName(),
                "contentTypeId", node.getAttraction().getContentTypeId()
        );
    }
}
