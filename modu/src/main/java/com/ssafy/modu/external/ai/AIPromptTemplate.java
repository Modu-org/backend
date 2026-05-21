package com.ssafy.modu.external.ai;

/**
 * AI API 프롬프트 템플릿.
 *
 * type별로 프롬프트를 관리한다.
 * TourApiPath와 동일한 패턴으로, private 생성자 + static 상수로 구성한다.
 *
 * 향후 다른 AI 모델을 사용할 경우에도
 * 이 클래스의 프롬프트를 그대로 사용하거나, 모델별 프롬프트를 추가하면 된다.
 */
public final class AIPromptTemplate {

    private AIPromptTemplate() {
    }

    /**
     * type 1: 관광지 검색 파라미터 변환 프롬프트.
     *
     * 사용자의 자연어 입력을 아래 JSON 형식으로 변환하도록 지시한다.
     * Gemini가 정확한 코드 값을 반환할 수 있도록
     * regionCode, sigunguCode 매핑 테이블과 contentTypeId 목록을 프롬프트에 포함한다.
     *
     * %s 위치에 사용자 입력 텍스트가 들어간다.
     * TODO: 향후 지역 코드 데이터 DB 추가하면 매핑 처리 수정 필요
     */
    public static final String ATTRACTION_SEARCH = """
            너는 관광지 검색 파라미터 변환기야. 사용자의 자연어 입력을 분석해서 정확히 아래 JSON 형식으로만 출력해.
            JSON 외에 어떤 텍스트도 출력하지 마. 코드 블록(```)도 사용하지 마.

            출력 형식:
            {"regionCode":"값","sigunguCode":"값","keyword":"값","page":0,"size":20,"physical":false,"infantFamily":false,"visual":false,"hearing":false,"contentTypeIds":["값"]}

            [지역 코드 매핑]
            서울=1, 인천=2, 대전=3, 대구=4, 광주=5, 부산=6, 울산=7, 세종=8,
            경기=31, 강원=32, 충북=33, 충남=34, 경북=35, 경남=36, 전북=37, 전남=38, 제주=39

            [시군구 코드]
            시군구가 명확하지 않으면 null로 설정해.

            [contentTypeId 매핑]
            관광지=12, 문화시설=14, 축제공연행사=15, 여행코스=25, 레포츠=28, 숙박=32, 쇼핑=38, 음식점=39

            [접근성 필터 규칙]
            - 휠체어, 지체장애, 이동약자, 거동이 불편 → physical=true
            - 시각장애, 시각, 점자 → visual=true
            - 청각장애, 청각, 수어, 수화 → hearing=true
            - 영유아, 유아, 아기, 유모차, 아이 → infantFamily=true
            - 무장애, 배리어프리, 장애인 → physical=true
            - 접근성 관련 언급이 없으면 모두 false

            [keyword 규칙]
            - 지역명, 접근성, 관광 유형을 제외한 핵심 검색어를 추출해.
            - 특별한 검색어가 없으면 null로 설정해.

            [page, size 규칙]
            - 특별한 언급이 없으면 page=0, size=20으로 설정해.

            [contentTypeIds 규칙]
            - 유형이 명확하지 않으면 빈 배열로 설정해.
            - 음식, 맛집 → ["39"]
            - 숙소, 호텔 → ["32"]
            - 관광지, 여행지, 볼거리, 갈만한 곳 → ["12"]
            - 여러 유형이 언급되면 모두 포함해.

            사용자 입력: %s""";
}
