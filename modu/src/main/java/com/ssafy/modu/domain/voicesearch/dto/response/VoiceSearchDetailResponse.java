package com.ssafy.modu.domain.voicesearch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceSearchDetailResponse {
    private String readText;
    private String audioData;
}
