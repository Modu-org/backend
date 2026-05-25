package com.ssafy.modu.external.tourapi;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

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

    public JsonNode getSync(String modifiedTime, int pageNo, int numOfRows, String showFlag) {
        Map<String, String> params = defaultParams(pageNo, numOfRows);
        // modifiedtime은 null/blank일 때 파라미터에 넣지 않는다.
        // (API가 빈 값으로 처리하는 방식이 불명확하고, 배치에서 "전체 목록"과 "변경분 동기화"를 명확히 분리하기 위함)
        if (modifiedTime != null && !modifiedTime.isBlank()) {
            params.put("modifiedtime", modifiedTime);
        }
        params.put("showflag", defaultIfBlank(showFlag, "1"));
        params.put("arrange", "C");
        return client.callKor(TourApiPath.SYNC_LIST, params);
    }

    public JsonNode getAccessibleSync(String modifiedTime, int pageNo, int numOfRows, String showFlag) {
        Map<String, String> params = defaultParams(pageNo, numOfRows);
        // KorWithService2(무장애) 목록 동기화. modifiedtime 처리 규칙은 일반 목록과 동일.
        if (modifiedTime != null && !modifiedTime.isBlank()) {
            params.put("modifiedtime", modifiedTime);
        }
        params.put("showflag", defaultIfBlank(showFlag, "1"));
        params.put("arrange", "C");
        return client.callKorWith(TourApiPath.SYNC_LIST, params);
    }

    private Map<String, String> defaultParams(int pageNo, int numOfRows) {
        Map<String, String> map = new HashMap<>();
        map.put("numOfRows", String.valueOf(numOfRows));
        map.put("pageNo", String.valueOf(pageNo));
        return map;
    }

    public JsonNode getAllLDongCodes() {
        Map<String, String> params = defaultParams(1, 1000);

        // 전체 시도 + 시군구 목록 조회
        params.put("lDongListYn", "Y");

        return client.callKor(TourApiPath.LDONG_CODE, params);
    }
}
