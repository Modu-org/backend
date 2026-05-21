package com.ssafy.modu.domain.voicesearch.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VoiceSearchParsedResult {

    private String regionCode;
    private String sigunguCode;
    private String keyword;

    private Integer page;
    private Integer size;

    private Boolean physical;
    private Boolean infantFamily;
    private Boolean visual;
    private Boolean hearing;

    private List<String> contentTypeIds;
}
