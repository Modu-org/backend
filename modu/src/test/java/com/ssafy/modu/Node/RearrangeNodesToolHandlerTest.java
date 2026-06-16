package com.ssafy.modu.Node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionContext;
import com.ssafy.modu.domain.aicommand.dto.tool.ToolExecutionResult;
import com.ssafy.modu.domain.aicommand.tool.RearrangeNodesToolHandler;
import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.attraction.repository.AttractionRepository;
import com.ssafy.modu.domain.node.entity.Node;
import com.ssafy.modu.domain.node.repository.NodeRepository;
import com.ssafy.modu.domain.schedule.entity.Schedule;
import com.ssafy.modu.domain.schedule.repository.ScheduleRepository;
import com.ssafy.modu.domain.user.entity.User;
import com.ssafy.modu.domain.user.entity.UserDetail;
import com.ssafy.modu.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class RearrangeNodesToolHandlerTest {

    @Autowired
    RearrangeNodesToolHandler rearrangeNodesToolHandler;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    AttractionRepository attractionRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    EntityManager em;

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void swapNodes() throws Exception {
        // given
        LocalDate date = LocalDate.parse("2026-06-13");

        User user = User.builder()
                .userName("test-user")
                .password("password")
                .nickname("tester")
                .build();

        UserDetail userDetail = UserDetail.builder()
                .physical(false)
                .infantFamily(false)
                .visual(false)
                .hearing(false)
                .build();

        user.setUserDetail(userDetail);
        userRepository.save(user);

        Schedule schedule = Schedule.create(
                user,
                "테스트 일정",
                date,
                date
        );

        scheduleRepository.save(schedule);

        Attraction attraction1 = saveAttraction("content-1", "관광지1");
        Attraction attraction2 = saveAttraction("content-2", "관광지2");
        Attraction attraction3 = saveAttraction("content-3", "관광지3");
        Attraction attraction4 = saveAttraction("content-4", "관광지4");

        Node node1 = createScheduledNode(attraction1, date, 1);
        Node node2 = createScheduledNode(attraction2, date, 2);
        Node node3 = createScheduledNode(attraction3, date, 3);
        Node node4 = createScheduledNode(attraction4, date, 4);

        schedule.addNode(node1);
        schedule.addNode(node2);
        schedule.addNode(node3);
        schedule.addNode(node4);

        em.flush();
        em.clear();

        List<Node> beforeNodes =
                nodeRepository.findAllBySchedule_IdAndVisitDateOrderByVisitOrderAscIdAsc(
                        schedule.getId(),
                        date
                );

        Node beforeOrder1 = findByVisitOrder(beforeNodes, 1);
        Node beforeOrder2 = findByVisitOrder(beforeNodes, 2);
        Node beforeOrder3 = findByVisitOrder(beforeNodes, 3);
        Node beforeOrder4 = findByVisitOrder(beforeNodes, 4);

        Long beforeOrder1NodeId = beforeOrder1.getId();
        Long beforeOrder2NodeId = beforeOrder2.getId();
        Long beforeOrder3NodeId = beforeOrder3.getId();
        Long beforeOrder4NodeId = beforeOrder4.getId();

        ToolExecutionContext context = new ToolExecutionContext(
                user.getId(),
                schedule.getId(),
                date,
                true
        );

        JsonNode arguments = objectMapper.readTree("""
            {
              "operation": "SWAP",
              "sourceOrder": 3,
              "targetOrder": 4,
              "apply": true
            }
        """);

        // when
        ToolExecutionResult result =
                rearrangeNodesToolHandler.execute(context, arguments);

        em.flush();
        em.clear();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getToolName()).isEqualTo("rearrange_nodes");
        assertThat(result.getSchedule()).isNotNull();

        List<Node> afterNodes =
                nodeRepository.findAllBySchedule_IdAndVisitDateOrderByVisitOrderAscIdAsc(
                        schedule.getId(),
                        date
                );

        assertThat(afterNodes).hasSize(4);

        Node afterOrder1 = findByVisitOrder(afterNodes, 1);
        Node afterOrder2 = findByVisitOrder(afterNodes, 2);
        Node afterOrder3 = findByVisitOrder(afterNodes, 3);
        Node afterOrder4 = findByVisitOrder(afterNodes, 4);

        assertThat(afterOrder1.getId()).isEqualTo(beforeOrder1NodeId);
        assertThat(afterOrder2.getId()).isEqualTo(beforeOrder2NodeId);
        assertThat(afterOrder3.getId()).isEqualTo(beforeOrder4NodeId);
        assertThat(afterOrder4.getId()).isEqualTo(beforeOrder3NodeId);
    }

    private Node createScheduledNode(
            Attraction attraction,
            LocalDate visitDate,
            int visitOrder
    ) {
        Node node = Node.from(attraction);
        node.updateVisitInfo(visitOrder, visitDate);
        return node;
    }

    private Node findByVisitOrder(List<Node> nodes, int visitOrder) {
        return nodes.stream()
                .filter(node -> node.getVisitOrder() != null)
                .filter(node -> node.getVisitOrder().equals(visitOrder))
                .findFirst()
                .orElseThrow();
    }

    private Attraction saveAttraction(String contentId, String name) {
        Attraction attraction = Attraction.create(contentId, "12");

        attraction.updateFromApi(
                name,                      // name
                "테스트 주소",              // address
                null,                      // addressDetail
                null,                      // zipcode
                BigDecimal.valueOf(35.0),  // latitude
                BigDecimal.valueOf(128.0), // longitude
                "12",                      // contentTypeId
                null,                      // tel
                null,                      // firstImageUrl
                null,                      // thumbnailImageUrl
                null,                      // overview
                "35",                      // lDongRegnCd
                "35010",                   // lDongSignguCd
                null,                      // lclsSystm1
                null,                      // lclsSystm2
                null,                      // lclsSystm3
                LocalDateTime.now(),       // apiCreatedTime
                LocalDateTime.now(),       // apiModifiedTime
                null,                      // homepage
                true,                      // showFlag
                null                       // copyrightType
        );

        return attractionRepository.save(attraction);
    }
}