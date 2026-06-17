package com.ssafy.modu.domain.aicommand.util;

import com.ssafy.modu.domain.aicommand.dto.request.VoiceCommandRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VisibleAttractionResolver {

    // 만약에 attractionId가 이미 명시되어있거나, 관광지 목록에서, 사용자의 요구 조건에 맞는 관광지가 하나밖에 없으면 그걸 사용
    public Long resolveAttractionIdOrNull(VoiceCommandRequest request) {
        if (request.getAttractionId() != null) {
            return request.getAttractionId();
        }

        if (request.getVisibleAttractions() == null || request.getVisibleAttractions().isEmpty()) {
            return null;
        }

        String text = normalize(request.getText());

        List<VoiceCommandRequest.VisibleAttraction> matched = request.getVisibleAttractions().stream()
                .filter(attraction -> {
                    String name = normalize(attraction.getName());
                    return !name.isBlank() && text.contains(name);
                })
                .toList();

        if (matched.size() == 1) {
            return matched.get(0).getAttractionId();
        }

        return null;
    }
    // 사용자가 요구하는 관광지가 여러 후보로 매핑되는지 확인 -> 음성 명령을 다시 받아와야함
    public boolean hasMultipleMatches(VoiceCommandRequest request) {
        if (request.getVisibleAttractions() == null || request.getVisibleAttractions().isEmpty()) {
            return false;
        }

        String text = normalize(request.getText());

        long count = request.getVisibleAttractions().stream()
                .filter(attraction -> {
                    String name = normalize(attraction.getName());
                    return !name.isBlank() && text.contains(name);
                })
                .count();

        return count > 1;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase();
    }
}
