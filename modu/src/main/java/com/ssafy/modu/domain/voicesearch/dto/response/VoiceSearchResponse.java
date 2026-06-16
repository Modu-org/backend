package com.ssafy.modu.domain.voicesearch.dto.response;

import com.ssafy.modu.domain.attraction.dto.response.AttractionListResponse;
import com.ssafy.modu.domain.attraction.dto.response.AttractionPageResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class VoiceSearchResponse {

    private List<AttractionListResponse> content;

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    private boolean first;
    private boolean last;
    private boolean empty;

    private ParsedFiltersResponse parsedFilters;

    public static VoiceSearchResponse of(AttractionPageResponse attractionPage, VoiceSearchParsedResult parsedResult) {
        if (attractionPage == null) {
            return null;
        }
        return VoiceSearchResponse.builder()
                .content(attractionPage.getContent())
                .page(attractionPage.getPage())
                .size(attractionPage.getSize())
                .totalElements(attractionPage.getTotalElements())
                .totalPages(attractionPage.getTotalPages())
                .first(attractionPage.isFirst())
                .last(attractionPage.isLast())
                .empty(attractionPage.isEmpty())
                .parsedFilters(ParsedFiltersResponse.from(parsedResult))
                .build();
    }
}
