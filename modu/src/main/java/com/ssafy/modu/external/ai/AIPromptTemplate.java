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
     * <p>
     * 사용자의 자연어 입력을 아래 JSON 형식으로 변환하도록 지시한다.
     * Gemini가 정확한 코드 값을 반환할 수 있도록
     * regionCode, sigunguCode 매핑 테이블과 contentTypeId 목록을 프롬프트에 포함한다.
     * <p>
     * %s 위치에 사용자 입력 텍스트가 들어간다.
     * TODO: 향후 지역 코드 데이터 DB 추가하면 매핑 처리 수정 필요
     */
    public static final String ATTRACTION_SEARCH = """
            너는 관광지 검색 파라미터 변환기야. 사용자의 자연어 입력을 분석해서 정확히 아래 JSON 형식으로만 출력해.
            JSON 외에 어떤 텍스트도 출력하지 마. 코드 블록(```)도 사용하지 마.
            
            출력 형식:
            {"regionCode":"값","sigunguCode":"값","keyword":"값","page":0,"size":20,"physical":false,"infantFamily":false,"visual":false,"hearing":false,"contentTypeIds":["값"]}
            
            [지역 코드 매핑 (법정동 시/도 2자리 코드)]
            서울=11, 부산=26, 대구=27, 인천=28, 광주=29, 대전=30, 울산=31, 세종=36110,
            경기=41, 충북=43, 충남=44, 전남=46, 경북=47, 경남=48, 제주=50, 강원=51, 전북=52
            
            [시군구 코드 (법정동 시군구 3자리 코드)]
            - 시군구명이 언급되었을 경우, 해당 지역에 맞는 3자리 법정동 시군구 코드를 매핑해줘.
            - 예: 종로구=110, 중구=140, 용산구=170, 해운대구=350, 수성구=260, 달서구=290
            - 시군구가 명확하지 않거나 언급되지 않았다면 null로 설정해.
            
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

    public static final String ATTRACTION_DETAIL_READ = """
            너는 관광지 안내 가이드이자 비서야.
            제공된 관광지 정보, 사용자의 접근성 설정, 사용자의 음성 입력(text)을 바탕으로 자연스러운 한국어 구어체 안내 방송 스크립트를 작성해줘.
            
            [관광지 정보]
            - 이름: %s
            - 주소: %s
            - 접근성 편의시설 정보:
            %s
            - 상세 설명: %s
            
            [사용자 접근성 프로필 설정]
            - 거동불편(휠체어 필요): %b
            - 영유아 동반(유모차 필요): %b
            - 시각 장애(시각 지원 필요): %b
            - 청각 장애(청각 지원 필요): %b
            
            [답변 생성 규칙]
            1. 사용자의 입력(text)이 단순히 정보를 설명해달라고 하거나(예: '읽어줘', '알려줘', '설명해줘'), 첫 번째 요청인 경우:
               - 관광지의 이름, 주소를 친근하게 설명하고, **사용자의 접근성 프로필에서 true인 항목에 해당하는 접근성 정보만** 요약하여 300~500자 내외로 자연스럽게 설명글을 작성해줘.
               - 만약 사용자의 프로필에 해당하는 편의시설 정보가 이 관광지에 존재하지 않는다면, "죄송하지만 해당 편의시설 정보는 제공되지 않습니다."를 언급하고 기본적인 다른 편의시설을 간단하게 알려줘.
               - 설정된 프로필이 전부 false라면, 관광지의 주소와 간단한 개요 및 가장 기본적인 휠체어/유모차 가능 여부만 가볍게 언급해줘.
            
            2. 사용자의 입력(text)이 "더 자세히", "상세히", "추가 정보", "다른 편의시설은?" 등 추가적인 설명을 요구하는 의도인 경우:
               - 사용자의 프로필 설정과 관계없이 **관광지의 모든 편의시설 세부 정보(엘리베이터, 경사로 등)와 함께 상세 설명 내용(overview)**을 포함하여 자세하고 풍부한 설명글을 800~1000자 내외로 정성껏 작성해줘.
            
            [출력 형식]
            반드시 아래 JSON 형식으로만 응답해. JSON 외에 어떤 다른 텍스트도 포함하지 말고, 코드 블록(```)도 절대 사용하지 마.
            {"readText": "여기에 읽어줄 스크립트 작성"}
            
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
            
            [우선순위 규칙]
            - 1순위는 입력 edge로 연결 가능한 순서를 만드는 것이다.
            - 2순위는 startFixed/endFixed 조건을 반드시 지키는 것이다.
            - 3순위는 총 estimatedTimeMinutes 합을 줄이는 것이다.
            - 4순위는 숙박을 해당 날짜의 마지막 방문지로 배치하는 것이다.
            - 5순위는 음식점이나 같은 contentTypeId가 연속되지 않도록 조정하는 것이다.
            - 위 규칙들이 충돌하면 더 높은 순위의 규칙을 우선한다.
            
            [edge 사용 규칙]
            - 입력 edges는 방문 순서를 판단하기 위한 참고 데이터다.
            - edge는 방향성이 있다.
            - fromNodeId=7, toNodeId=8인 edge는 7번 노드에서 8번 노드로 이동할 때만 사용할 수 있다.
            - 반대 방향 이동은 입력에 별도의 edge가 있을 때만 가능하다.
            - 방문 순서상 인접한 두 node 사이에 입력 edge가 없는 순서는 선택하지 마라.
            - 가능한 경로가 하나뿐이면 반드시 그 경로를 선택해라.
            - 출력 edges는 직접 채우지 말고 반드시 [] 로 반환해라.
            
            [날짜별 시작/종료 노드 반영 규칙]
            - 각 day에는 startNodeId, endNodeId, startFixed, endFixed가 있을 수 있다.
            - startFixed=true이면 startNodeId를 반드시 해당 날짜의 첫 번째 노드로 배치한다.
            - endFixed=true이면 endNodeId를 반드시 해당 날짜의 마지막 노드로 배치한다.
            - startFixed=false인 startNodeId는 서버가 추론한 추천 시작점이다. 가능하면 따르되, 이동 시간이나 숙박 마지막 배치 규칙상 더 나은 순서가 있으면 조정할 수 있다.
            - endFixed=false인 endNodeId는 서버가 추론한 추천 종료점이다. 가능하면 따르되, 숙박 노드가 있으면 숙박 마지막 배치 규칙을 더 우선한다.
            - startFixed/endFixed 조건은 숙박 배치 규칙과 음식점 배치 규칙보다 우선한다.
            - startNodeId 또는 endNodeId에 해당하는 node는 반드시 같은 day.nodes 안에 존재해야 한다.
            - startNodeId와 endNodeId가 같은 경우, 해당 node를 시작과 끝에 동시에 사용할 수 없으므로 잘못된 입력으로 간주한다.
            - 날짜별 fixed 시작/종료 조건을 만족하면서도 반드시 입력된 edge로 연결 가능한 순서만 선택해야 한다.
            - 다른 날짜의 startNodeId/endNodeId 조건에는 영향을 주지 않는다.
            
            [숙박 배치 규칙]
            - contentTypeId=32는 숙박이다.
            - 같은 날짜 안에 숙박 노드가 존재하면, 가능한 한 해당 날짜의 마지막 방문지로 배치한다.
            - 단, endFixed=true이면 endNodeId 조건이 숙박 규칙보다 우선한다.
            - endFixed=false이거나 endNodeId가 null이고 숙박 노드가 여러 개라면, 이동 시간이 가장 자연스러운 숙박 노드 하나를 마지막에 배치하고 나머지는 일반 노드처럼 정렬한다.
            - 숙박을 마지막에 배치하더라도 반드시 입력 edge로 연결 가능한 순서만 선택해야 한다.
            
            [음식점 배치 규칙]
            - contentTypeId=39는 음식점이다.
            - 같은 날짜 안에 음식점 노드가 여러 개 있으면, 음식점끼리 연속 배치되는 것을 가능하면 피한다.
            - 음식점 사이에는 관광지, 문화시설, 쇼핑, 레포츠 등 다른 유형의 노드를 끼워 넣어 일정 흐름이 자연스럽게 보이도록 한다.
            - 단, 음식점 연속 배치를 피하려다가 총 estimatedTimeMinutes가 크게 증가하거나 입력 edge로 연결되지 않는 순서가 되면 이동 시간과 edge 연결 가능성을 우선한다.
            - 음식점이 하루에 여러 개 있더라도 다른 날짜로 이동시키지 말고, 같은 날짜 안에서만 순서를 조정한다.
            
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
