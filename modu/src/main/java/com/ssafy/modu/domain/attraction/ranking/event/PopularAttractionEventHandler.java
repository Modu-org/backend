package com.ssafy.modu.domain.attraction.ranking.event;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PopularAttractionEventHandler {

    private final StringRedisTemplate redisTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void increaseRanking(AttractionAddedToScheduleEvent event) {
        String key = "ranking:region:" + event.regionCode() + ":attractions";

        redisTemplate.opsForZSet()
                .incrementScore(key, event.attractionId().toString(), 1);
    }
}