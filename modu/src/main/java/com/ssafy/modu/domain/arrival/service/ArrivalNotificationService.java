package com.ssafy.modu.domain.arrival.service;

import com.ssafy.modu.domain.arrival.entity.ArrivalLog;
import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.caregiver.entity.CaregiverRelation;
import com.ssafy.modu.domain.caregiver.repository.CaregiverRelationRepository;
import com.ssafy.modu.domain.node.entity.Node;
import com.ssafy.modu.domain.notification.entity.Notification;
import com.ssafy.modu.domain.notification.entity.enums.NotificationType;
import com.ssafy.modu.domain.notification.service.FcmService;
import com.ssafy.modu.domain.notification.service.NotificationService;
import com.ssafy.modu.domain.schedule.entity.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArrivalNotificationService {

    private final FcmService fcmService;
    private final NotificationService notificationService;
    private final CaregiverRelationRepository caregiverRelationRepository;

    public void notifyCaregivers(
            Schedule schedule,
            Node currentNode,
            Node nextNode,
            ArrivalLog arrivalLog,
            boolean arrived,
            double distanceMeters
    ) {
        // 만약에 공유 설정이 되지 않은 스케줄이면 그냥 패스
        if (!schedule.isArrivalShared()) {
            return;
        }

        // 이 스케줄의 여행자의 보호자 ID를 리스트로 가져옴
        List<Long> caregiverIds = caregiverRelationRepository
                .findAllByTravelerIdAndActiveTrue(schedule.getUser().getId())
                .stream()
                .map(CaregiverRelation::getCaregiverId)
                .toList();

        String title = createTitle(arrived);
        String body = createBody(currentNode, nextNode, arrived, distanceMeters);

        for (Long caregiverId : caregiverIds) {
            Notification notification = notificationService.createNotification(
                    caregiverId,
                    NotificationType.ARRIVAL,
                    title,
                    body,
                    arrivalLog.getId()
            );

            fcmService.sendToUser(
                    caregiverId,
                    title,
                    body,
                    Map.of(
                            "type", "ARRIVAL",
                            "notificationId", String.valueOf(notification.getId()),
                            "arrivalLogId", String.valueOf(arrivalLog.getId()),
                            "scheduleId", String.valueOf(schedule.getId()),
                            "nodeId", String.valueOf(currentNode.getId())
                    )
            );
        }
    }

    private String createTitle(boolean arrived) {
        return arrived ? "도착 확인 완료" : "도착 확인 실패";
    }

    private String createBody(
            Node currentNode,
            Node nextNode,
            boolean arrived,
            double distanceMeters
    ) {
        Attraction currentAttraction = currentNode.getAttraction();
        String currentName = currentAttraction.getName();

        if (!arrived) {
            return currentName + " 기준 "
                    + Math.round(distanceMeters)
                    + "m 떨어진 위치에서 도착 버튼이 눌렸습니다.";
        }

        if (nextNode == null) {
            return currentName + "에 도착했습니다. 다음 목적지는 없습니다.";
        }

        return currentName + "에 도착했습니다. 다음 목적지는 "
                + nextNode.getAttraction().getName()
                + "입니다.";
    }
}