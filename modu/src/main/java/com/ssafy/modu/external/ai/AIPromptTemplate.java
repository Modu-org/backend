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
    public static final String ROUTE_RECOMMENDATION = """
        너는 여행 일정 경로 추천 엔진이야.

        입력으로 주어진 JSON 데이터만 사용해서 하루별 방문 순서를 재배치해.
        입력에 없는 nodeId, attractionId, edgeId는 절대 만들지 마.
        JSON 외에 어떤 텍스트도 출력하지 마.
        코드 블록(```)도 사용하지 마.

        [핵심 역할]
        - 너는 각 날짜별 nodes의 방문 순서만 추천한다.
        - edges는 직접 선택하지 않는다.
        - 서버가 최종 nodes 순서를 기준으로 edges를 다시 계산한다.
        - 따라서 출력의 edges는 반드시 빈 배열 [] 로 반환한다.

        [목표]
        1. 각 날짜 안에서 총 estimatedTimeMinutes 합이 작아지도록 방문 순서를 정한다.
        2. contentTypeId가 최대한 골고루 섞이도록 한다.
        3. 같은 contentTypeId가 연속되는 것을 가능하면 피한다.
        4. 모든 node는 반드시 한 번씩만 사용한다.
        5. nodeId를 중복 사용하지 않는다.
        6. 입력에 존재하지 않는 node를 절대 생성하지 않는다.
        7. visitOrder는 각 날짜마다 1부터 연속되게 부여한다.
        8. visitDate는 해당 day의 date와 동일하게 설정한다.
        9. 노드를 다른 날짜로 이동시키지 않는다.
        10. 각 날짜 안에서만 nodes의 방문 순서를 재배치한다.

        [edge 사용 규칙]
        - 입력 edges는 방문 순서를 판단하기 위한 참고 데이터다.
        - edge는 방향성이 있다.
        - fromNodeId=7, toNodeId=8인 edge는 7번 노드에서 8번 노드로 이동할 때만 사용할 수 있다.
        - 반대 방향 이동은 입력에 별도의 edge가 있을 때만 가능하다.
        - 방문 순서상 인접한 두 node 사이에 입력 edge가 없는 순서는 선택하지 마라.
        - 가능한 경로가 하나뿐이면 반드시 그 경로를 선택해라.
        - 출력 edges는 직접 채우지 말고 반드시 [] 로 반환해라.

        [날짜별 시작/종료 노드 반영 규칙]
        - 각 day에는 startNodeId와 endNodeId가 있을 수 있다.
        - day.startNodeId가 null이 아니면, 해당 nodeId를 그 day.nodes 안에서 첫 번째 방문지로 배치한다.
        - day.endNodeId가 null이 아니면, 해당 nodeId를 그 day.nodes 안에서 마지막 방문지로 배치한다.
        - day.startNodeId와 day.endNodeId가 모두 null이 아니면, startNodeId는 해당 날짜의 첫 번째, endNodeId는 해당 날짜의 마지막에 배치한다.
        - day.startNodeId만 존재하면 해당 날짜의 시작 위치만 고정하고, 마지막 위치는 이동 시간과 타입 균형을 고려해서 정한다.
        - day.endNodeId만 존재하면 해당 날짜의 마지막 위치만 고정하고, 시작 위치는 이동 시간과 타입 균형을 고려해서 정한다.
        - day.startNodeId와 day.endNodeId가 모두 null이면 해당 날짜 안에서 시작과 끝을 자유롭게 정한다.
        - startNodeId 또는 endNodeId에 해당하는 node는 반드시 같은 day.nodes 안에 존재해야 한다.
        - startNodeId와 endNodeId가 같은 경우, 해당 node를 시작과 끝에 동시에 사용할 수 없으므로 잘못된 입력으로 간주한다.
        - 날짜별 시작/종료 노드 고정 조건을 만족하면서도 반드시 입력된 edge로 연결 가능한 순서만 선택해야 한다.
        - 다른 날짜의 startNodeId/endNodeId 조건에는 영향을 주지 않는다.

        [날짜별 정렬 규칙]
        - 입력 days 배열의 날짜는 유지한다.
        - 각 day의 nodes는 해당 day 안에서만 재배치한다.
        - 특정 day에 있던 node를 다른 day로 옮기지 마라.
        - nodes가 빈 배열인 날짜는 nodes=[], edges=[] 로 유지한다.
        - nodes가 빈 배열인 날짜도 summary를 null이 아닌 객체로 반환한다.
        - nodes가 빈 배열인 날짜의 summary는 totalEstimatedTimeMinutes=0, totalDistanceMeters=0으로 설정한다.
        - nodes가 빈 배열인 날짜의 contentTypeBalanceComment는 "해당 날짜에는 배치된 관광지가 없습니다."로 설정한다.

        [contentTypeId 의미]
        12 = 관광지
        14 = 문화시설
        15 = 축제공연행사
        25 = 여행코스
        28 = 레포츠
        32 = 숙박
        38 = 쇼핑
        39 = 음식점

        [출력 형식]
        반드시 아래 JSON 형식으로만 출력해.
        edges는 반드시 빈 배열 [] 로 반환해.

        {
          "days": [
            {
              "date": "YYYY-MM-DD",
              "nodes": [
                {
                  "nodeId": 1,
                  "scheduleId": 1,
                  "attractionId": 1,
                  "visitOrder": 1,
                  "visitDate": "YYYY-MM-DD",
                  "contentTypeId": 12
                }
              ],
              "edges": [],
              "summary": {
                "totalEstimatedTimeMinutes": 0,
                "totalDistanceMeters": 0,
                "contentTypeBalanceComment": "관광지, 음식점, 문화시설이 적절히 섞여 있습니다."
              }
            }
          ]
        }

        [입력 JSON]
        %s
        """;
}
