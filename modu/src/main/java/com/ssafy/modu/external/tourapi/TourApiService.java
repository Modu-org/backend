package com.ssafy.modu.external.tourapi;

import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TourApiService {

    private final TourApiClient client;

    public JsonNode getCommon(String contentId) {
        Map<String, String> params = defaultParams();
        params.put("contentId", contentId);

        return client.callKor(TourApiPath.DETAIL_COMMON, params);
    }

    public JsonNode getAccessibility(String contentId) {
        Map<String, String> params = defaultParams();
        params.put("contentId", contentId);

        return client.callKorWith(TourApiPath.DETAIL_WITH_TOUR, params);
    }

    public JsonNode getSync(String modifiedTime) {
        Map<String, String> params = defaultParams();
        params.put("modifiedtime", modifiedTime);
        params.put("showflag", "1");

        return client.callKor(TourApiPath.SYNC_LIST, params);
    }

    private Map<String, String> defaultParams() {
        Map<String, String> map = new HashMap<>();
        map.put("numOfRows", "10");
        map.put("pageNo", "1");
        return map;
    }
}
