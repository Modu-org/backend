package com.ssafy.modu.domain.attraction.ranking.service;

import com.ssafy.modu.domain.attraction.entity.Attraction;
import com.ssafy.modu.domain.attraction.ranking.dto.PopularAttractionResponse;
import com.ssafy.modu.domain.attraction.repository.AttractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PopularAttractionQueryService {

    private final StringRedisTemplate redisTemplate;
    private final AttractionRepository attractionRepository;

    public List<PopularAttractionResponse> getPopularAttractions(String regionCode, int limit) {
        String key = "ranking:region:" + regionCode + ":attractions";

        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet()
                        .reverseRangeWithScores(key, 0, limit - 1);

        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        List<Long> attractionIds = tuples.stream()
                .map(tuple -> Long.valueOf(tuple.getValue()))
                .toList();

        Map<Long, Attraction> attractionMap = attractionRepository.findAllById(attractionIds)
                .stream()
                .collect(Collectors.toMap(Attraction::getId, Function.identity()));

        return tuples.stream()
                .map(tuple -> {
                    Long attractionId = Long.valueOf(tuple.getValue());
                    Attraction attraction = attractionMap.get(attractionId);

                    if (attraction == null) {
                        return null;
                    }

                    long count = tuple.getScore() == null
                            ? 0L
                            : tuple.getScore().longValue();

                    return PopularAttractionResponse.from(attraction, count);
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
