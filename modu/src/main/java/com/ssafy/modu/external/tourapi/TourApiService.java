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
        Map<String, String> params = defaultParams(1, 10);
        params.put("contentId", contentId);
        return client.callKor(TourApiPath.DETAIL_COMMON, params);
    }

    public JsonNode getIntro(String contentId, String contentTypeId) {
        Map<String, String> params = defaultParams(1, 10);
        params.put("contentId", contentId);
        params.put("contentTypeId", contentTypeId);
        return client.callKor(TourApiPath.DETAIL_INTRO, params);
    }

    public JsonNode getInfo(String contentId, String contentTypeId) {
        Map<String, String> params = defaultParams(1, 50);
        params.put("contentId", contentId);
        params.put("contentTypeId", contentTypeId);
        return client.callKor(TourApiPath.DETAIL_INFO, params);
    }

    public JsonNode getAccessibility(String contentId) {
        Map<String, String> params = defaultParams(1, 10);
        params.put("contentId", contentId);
        return client.callKorWith(TourApiPath.DETAIL_WITH_TOUR, params);
    }

    public JsonNode getSync(String modifiedTime) {
        return getSync(modifiedTime, 1, 10, "1");
    }

    public JsonNode getSync(String modifiedTime, int pageNo, int numOfRows, String showFlag) {
        Map<String, String> params = defaultParams(pageNo, numOfRows);
        params.put("modifiedtime", modifiedTime);
        params.put("showflag", showFlag);
        params.put("arrange", "C");
        return client.callKor(TourApiPath.SYNC_LIST, params);
    }

    private Map<String, String> defaultParams(int pageNo, int numOfRows) {
        Map<String, String> map = new HashMap<>();
        map.put("numOfRows", String.valueOf(numOfRows));
        map.put("pageNo", String.valueOf(pageNo));
        return map;
    }
}
