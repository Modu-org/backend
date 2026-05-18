package com.ssafy.modu.domain.attraction.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AttractionSearchRequest {

    private String regionCode;
    private String sigunguCode;
    private String keyword;

    private Integer page = 0;
    private Integer size = 20;

    private Boolean physical;
    private Boolean infantFamily;
    private Boolean visual;
    private Boolean hearing;

    private List<String> contentTypeIds;
}