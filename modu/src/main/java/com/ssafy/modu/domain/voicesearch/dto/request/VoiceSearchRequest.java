package com.ssafy.modu.domain.voicesearch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoiceSearchRequest {

    @NotBlank
    private String text;

    @NotNull
    private Integer type;
}
