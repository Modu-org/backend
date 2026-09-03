# MODU

**접근성 정보를 바탕으로 여행지를 찾고, AI로 일정을 구성하며, 보호자와 도착 소식을 공유하는 여행 서비스**

여행지를 고를 때 필요한 정보는 사람마다 다릅니다. 휠체어 이용자는 이동 편의시설을, 시각·청각장애인은 안내 시설을, 영유아 동반 가족은 유아 편의시설을 확인해야 합니다. MODU는 이러한 조건으로 관광지를 검색하고, 선택한 장소를 여행 일정과 연결하는 Spring Boot 기반 백엔드입니다.

관광 데이터 수집부터 접근성 검색, AI 일정 추천·편집, 위치 기반 도착 확인까지 하나의 흐름으로 구현했습니다.

| 핵심 과제 | 구현 방식 |
| --- | --- |
| 사용자에게 필요한 접근성 조건으로 관광지 찾기 | 접근성 데이터 정규화, 카테고리별 `EXISTS` 조건을 결합한 검색 |
| 여행 기간과 이동 동선을 고려한 일정 구성 | 서버의 날짜별 장소 배치 + 실제 경로 정보 + AI 방문 순서 추천 |
| 자연어로 검색하고 일정 수정하기 | Gemini 검색 조건 추출, OpenAI Tool Calling 기반 일정 작업 |
| 보호자에게 여행 진행 상황 전달하기 | 사용자 요청 좌표로 도착 확인, 공유 설정에 따른 FCM 알림 |

[주요 기능](#주요-기능) · [아키텍처](#아키텍처) · [설계와 구현](#설계와-구현) · [실행 방법](#실행-방법) · [주요 API](#주요-api) · [테스트](#테스트)

## 주요 기능

| 기능 | 설명 |
| --- | --- |
| 접근성 관광지 검색 | 지역·시군구·키워드·관광 유형과 지체·시각·청각·영유아 동반 조건을 조합해 검색합니다. |
| 관광지 상세·리뷰 | 관광지 소개, 위치, 접근성 시설 정보를 조회하고 리뷰를 작성·수정·삭제합니다. |
| 자연어 검색·음성 안내 | 입력 문장을 검색 조건으로 변환하고, 관광지 상세 정보를 사용자 접근성 설정에 맞춰 안내 문장과 음성으로 제공합니다. |
| 여행 일정 관리 | 여행 기간·인원·예산을 설정하고, 장소를 추가한 뒤 날짜와 방문 순서를 편집합니다. 날짜가 정해지지 않은 장소도 보관할 수 있습니다. |
| AI 일정 자동 배치 | 지정한 시작·종료 장소와 날짜 간 동선을 고려해 장소를 배치하고, 날짜별 방문 순서를 추천합니다. |
| AI 일정 명령 | “3번과 4번 순서를 바꿔줘”, “새 일정을 만들어 이 관광지를 추가해줘”와 같은 명령을 처리합니다. |
| 보호자 연결·도착 공유 | 보호자 연결 요청·수락·거절, 일정별 공유 설정, 도착 확인 결과와 다음 목적지 알림을 제공합니다. |
| 회원·인증 | 일반 회원가입·로그인, Google·Kakao·Naver 소셜 로그인, 토큰 갱신과 로그아웃을 지원합니다. |
| 지역별 인기 관광지 | 일정에 관광지가 추가된 횟수를 Redis Sorted Set으로 집계합니다. |

음성 관련 API의 입력은 클라이언트에서 전달한 **텍스트**입니다. 상세 안내에서는 서버가 TTS 음성을 생성해 Base64로 반환하며, 음성 합성이 실패하면 안내 텍스트를 반환합니다.

## 아키텍처

```mermaid
flowchart LR
    Client[클라이언트] --> API[Spring Boot REST API]
    API --> Auth[Spring Security / JWT / OAuth2]
    API --> Domain[검색 · 일정 · AI 명령 · 도착 공유]
    Domain --> DB[(MySQL)]
    Auth --> Redis[(Redis)]
    Domain --> Redis
    Domain --> AI[SSAFY GMS: OpenAI / Gemini]
    Domain --> Kakao[Kakao Mobility]
    Domain --> FCM[Firebase Cloud Messaging]
    FCM --> Caregiver[보호자 클라이언트]
    Tour[한국관광공사 TourAPI] --> Batch[초기 적재 / 변경분 동기화]
    Batch --> DB
```

Spring MVC 기반 API 서버이며, 외부 HTTP API 호출에는 `WebClient`를 사용합니다. MySQL에는 관광지·접근성·일정·경로 캐시·알림 데이터를 저장하고, Redis에는 인증 토큰 상태와 인기 관광지 순위를 저장합니다.

### 기술 스택

| 영역 | 기술 |
| --- | --- |
| 언어·프레임워크 | Java 17, Spring Boot 3.5.0 |
| 데이터 접근 | Spring Data JPA, MySQL 8.0 |
| 인증·인가 | Spring Security, OAuth2 Client, JJWT 0.12.6, BCrypt |
| 토큰 저장·랭킹 | Spring Data Redis, Redis 7 |
| AI | SSAFY GMS 경유 OpenAI `gpt-4o-mini`, Gemini `gemini-2.5-flash`, OpenAI TTS `gpt-4o-mini-tts` |
| 외부 연동 | 한국관광공사 TourAPI, Kakao Mobility, Firebase Admin SDK |
| API 문서 | springdoc-openapi 2.8.9 / Swagger UI |
| 빌드·실행 | Gradle Wrapper, Docker 멀티 스테이지 빌드, Docker Compose |
| 테스트 | JUnit 5, Mockito, Spring Boot Test, H2, k6 실행 스크립트 |

버전과 모델명은 저장소의 [빌드 설정](modu/build.gradle)과 [애플리케이션 설정](modu/src/main/resources/application.yaml)을 기준으로 작성했습니다.

## 설계와 구현

### 1. 접근성 검색: 선택한 조건을 모두 만족하도록 구성

접근성 정보는 관광지별로 카테고리, 시설 유형, 제공 상태, 설명을 나누어 저장합니다. 제공 상태는 `AVAILABLE`, `UNAVAILABLE`, `PARTIAL`, `UNKNOWN`, `NEED_CHECK`로 구분합니다.

검색에서는 선택한 카테고리마다 별도의 `EXISTS` 서브쿼리를 만들고 `AND`로 결합합니다. 예를 들어 `visual=true&hearing=true` 요청은 시각·청각 접근성 조건을 모두 만족하는 관광지만 반환합니다. 각 카테고리에 해당하는 핵심 시설 유형과 `AVAILABLE` 상태를 함께 검사해, 일반 주차장 정보만 있는 장소가 시각장애인 편의시설 검색에 포함되는 것을 방지합니다.

검색 조건은 요청에 명시된 값을 사용합니다. 목록에 포함된 관광지들의 접근성 정보는 ID 목록으로 일괄 조회해 응답에 결합합니다.

코드: [AttractionSpecification](modu/src/main/java/com/ssafy/modu/domain/attraction/repository/specification/AttractionSpecification.java) · [AttractionService](modu/src/main/java/com/ssafy/modu/domain/attraction/service/AttractionService.java)

### 2. 일정 추천: 서버의 날짜 배치와 AI의 방문 순서 추천 분리

일정은 여행 전체를 나타내는 `Schedule`, 방문 장소인 `Node`, 장소 간 거리·이동 시간을 담는 `Edge`로 구성합니다.

자동 배치는 다음 순서로 진행합니다.

1. 일정 소유자와 요청 조건을 확인하고, 사용자가 지정한 날짜별 시작·종료 장소를 고정합니다.
2. 빈 날짜에 기준 장소를 배치한 뒤, 장소 간 거리·전후 날짜의 연결성·날짜별 배치 수를 고려해 나머지 장소를 배치합니다.
3. 장소 간 경로 정보를 확보하고, 날짜별 장소와 경로를 AI에 전달합니다.
4. AI가 제안한 결과의 노드 집합, 날짜 유지 여부, 방문 순서, 고정 시작·종료 조건을 서버에서 검증합니다.
5. 검증한 방문 순서를 일정에 반영합니다.

날짜 배정은 서버가 담당하고 AI는 각 날짜 안의 방문 순서를 추천합니다. 경로 거리와 이동 시간은 Kakao Mobility 자동차 길찾기 기준이며, 휠체어·보행 전용 경로를 계산하는 기능은 포함하지 않습니다.

코드: [RouteRecommendService](modu/src/main/java/com/ssafy/modu/domain/routerecommend/service/RouteRecommendService.java) · [AIService](modu/src/main/java/com/ssafy/modu/external/ai/AIService.java)

### 3. 경로 재사용: 일정 간 캐시 공유와 변경 구간 계산

경로 캐시는 MySQL에 `(출발 관광지, 도착 관광지, 제공자)` 단위로 저장합니다. 같은 관광지 구간을 다른 일정에서 사용하더라도 기존 거리·이동 시간 정보를 재사용하며, 캐시가 없는 구간만 Kakao Mobility에 요청합니다. 방향을 구분하므로 `A → B`와 `B → A`는 별도 경로입니다.

수동 일정 편집에서는 변경된 날짜의 최종 방문 순서를 기준으로 필요한 인접 구간을 확인합니다. 예를 들어 `A → B → C`를 `A → C → B`로 바꾸면 새로 필요한 `A → C`, `C → B` 구간을 확보합니다. 캐시 적중·미적중과 외부 API 호출 횟수도 결과에 집계합니다.

코드: [EdgeService](modu/src/main/java/com/ssafy/modu/domain/edge/service/EdgeService.java) · [NodeService](modu/src/main/java/com/ssafy/modu/domain/node/service/NodeService.java) · [AttractionRouteCache](modu/src/main/java/com/ssafy/modu/domain/routecache/entity/AttractionRouteCache.java)

### 4. AI 명령: 등록된 도구를 통한 일정 작업

자연어 명령은 `AiScheduleCommandService → ScheduleToolRegistry → ScheduleToolExecutor → ToolHandler` 흐름으로 실행합니다. 일정 조회·생성, 관광지 추가, 노드 검색·재정렬, 자동 배치를 각각의 핸들러로 분리했습니다.

특정 일정 안에서 실행하는 명령과 여러 일정에 걸친 작업을 구분하고, 실행 범위에 허용된 도구만 제공합니다. 도구 실행 결과를 다음 AI 요청에 전달해 여러 단계의 작업을 처리하며, 호출 라운드는 최대 10회로 제한합니다. 처리 중 예외가 발생하면 트랜잭션을 롤백 대상으로 표시하고 사용자 안내 메시지를 반환합니다.

코드: [AiScheduleCommandService](modu/src/main/java/com/ssafy/modu/domain/aicommand/service/AiScheduleCommandService.java) · [ScheduleToolRegistry](modu/src/main/java/com/ssafy/modu/domain/aicommand/service/ScheduleToolRegistry.java) · [Tool Handler 모음](modu/src/main/java/com/ssafy/modu/domain/aicommand/tool)

### 5. 관광 데이터 수집: 커서 기반 적재와 변경분 동기화

TourAPI의 일반 관광 정보와 무장애 관광 정보를 수집합니다. 초기 전체 적재와 변경분 동기화의 작업 상태를 분리하고, 다음 페이지·처리 상태·동기화 기준 시각을 DB에 저장합니다. 처리량 제한 등으로 완료하지 못한 작업은 저장된 커서를 기준으로 이어서 실행합니다.

관광지 목록과 상세·접근성 정보 수집을 분리하고, 상세 응답에 데이터가 없는 항목은 `NO_DATA` 상태로 관리합니다. 원본에서 제거된 무장애 관광지를 확인하는 주기 작업도 포함합니다. 배치는 Spring의 `@Scheduled` 기반으로 동작합니다.

코드: [TourInitialLoadService](modu/src/main/java/com/ssafy/modu/batch/tour/service/TourInitialLoadService.java) · [TourModifiedSyncService](modu/src/main/java/com/ssafy/modu/batch/tour/service/TourModifiedSyncService.java) · [TourBatchCursor](modu/src/main/java/com/ssafy/modu/batch/tour/cursor/TourBatchCursor.java)

### 6. 도착 공유·인증·랭킹의 상태 관리

| 영역 | 구현 내용 |
| --- | --- |
| 도착 확인 | 사용자가 도착 확인을 요청한 좌표와 관광지 좌표의 거리를 계산합니다. 100m 이내이면 도착으로 처리하고 확인 이력을 저장합니다. |
| 공유 범위 | 일정의 공유 설정이 켜져 있을 때 수락된 보호자에게 확인 결과를 전달합니다. 도착 이력 조회에서도 본인 여부 또는 보호자 관계와 공유 설정을 확인합니다. |
| 중복 요청 | 이미 도착에 성공한 장소를 다시 확인하면 기존 성공 결과와 다음 목적지를 반환합니다. |
| 인증 상태 | Access Token은 Bearer 인증에 사용하고, Refresh Token은 HttpOnly 쿠키와 Redis의 사용자·세션별 키로 관리합니다. 로그아웃한 Access Token은 남은 유효 시간 동안 블랙리스트에 저장합니다. |
| 인기 순위 | 관광지 추가 트랜잭션이 커밋된 뒤 `AFTER_COMMIT` 이벤트에서 지역별 Redis Sorted Set 점수를 증가시킵니다. |

코드: [ArrivalService](modu/src/main/java/com/ssafy/modu/domain/arrival/service/ArrivalService.java) · [ArrivalNotificationService](modu/src/main/java/com/ssafy/modu/domain/arrival/service/ArrivalNotificationService.java) · [AuthService](modu/src/main/java/com/ssafy/modu/domain/user/service/AuthService.java) · [PopularAttractionEventHandler](modu/src/main/java/com/ssafy/modu/domain/attraction/ranking/event/PopularAttractionEventHandler.java)

## 프로젝트 구조

```text
.
├── README.md
└── modu/
    ├── build.gradle
    ├── Dockerfile
    ├── docker-compose.yml
    ├── auto-arrange-test.js
    └── src/
        ├── main/
        │   ├── java/com/ssafy/modu/
        │   │   ├── domain/       # 관광지, 접근성, 일정, AI 명령, 보호자, 알림
        │   │   ├── external/     # TourAPI, AI, Kakao Mobility 클라이언트
        │   │   ├── batch/tour/   # 관광 데이터 수집, 커서, 스케줄러
        │   │   ├── admin/tour/   # 데이터 적재·동기화 API
        │   │   └── global/       # 인증, 공통 응답, 예외 처리, 설정
        │   └── resources/application.yaml
        └── test/java/com/ssafy/modu/
```

도메인별로 `controller`, `service`, `repository`, `entity`, `dto`를 구성하고, 외부 API 연동 코드는 `external` 패키지로 분리했습니다.

## 실행 방법

### 1. 사전 준비

- Docker와 Docker Compose
- 일반·무장애 관광 정보를 조회할 수 있는 TourAPI 서비스 키
- Kakao Mobility REST API 키와 SSAFY GMS 키
- Google·Kakao·Naver OAuth 클라이언트 설정
- Firebase 서비스 계정 정보

현재 설정은 외부 서비스 환경변수를 참조하며, 서버 시작 시 Firebase 서비스 계정을 초기화합니다. 아래 예시의 `REPLACE_ME`는 실제 발급값으로 바꿔야 합니다. `.env`는 Git 추적에서 제외되어 있습니다.

### 2. 환경변수 설정

저장소 루트에서 `modu`로 이동한 뒤 `.env` 파일을 생성합니다.

```sh
cd modu
```

<details>
<summary><strong>Docker Compose용 .env 예시</strong></summary>

```dotenv
MYSQL_ROOT_PASSWORD=REPLACE_ME
MYSQL_DATABASE=modu
MYSQL_USER=modu
MYSQL_PASSWORD=REPLACE_ME

SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/modu?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
SPRING_DATASOURCE_USERNAME=modu
SPRING_DATASOURCE_PASSWORD=REPLACE_ME
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379

JWT_SECRET=REPLACE_WITH_A_RANDOM_SECRET_OF_AT_LEAST_32_BYTES
TOUR_API_SERVICE_KEY=REPLACE_ME
KAKAO_REST_API_KEY=REPLACE_ME
GMS_KEY=REPLACE_ME

GOOGLE_CLIENT_ID=REPLACE_ME
GOOGLE_CLIENT_SECRET=REPLACE_ME
KAKAO_CLIENT_ID=REPLACE_ME
NAVER_CLIENT_ID=REPLACE_ME
NAVER_CLIENT_SECRET=REPLACE_ME

APP_COOKIE_SECURE=false
APP_COOKIE_SAME_SITE=Lax
OAUTH2_REDIRECT_URI=http://localhost:5173/oauth/success

FIREBASE_PROJECT_ID=REPLACE_ME
FIREBASE_CLIENT_EMAIL=REPLACE_ME
FIREBASE_PRIVATE_KEY_ID=REPLACE_ME
FIREBASE_CLIENT_ID=REPLACE_ME
FIREBASE_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\nREPLACE_ME\n-----END PRIVATE KEY-----\n
FIREBASE_TOKEN_URI=https://oauth2.googleapis.com/token
```

`MYSQL_PASSWORD`와 `SPRING_DATASOURCE_PASSWORD`는 같은 값으로 설정합니다. Firebase 개인키는 PEM 전체를 사용하고, 위 Compose 예시에서는 줄바꿈을 문자 `\n`으로 표현합니다.

OAuth 공급자에는 `http://localhost:8080/login/oauth2/code/{registrationId}`를 콜백 URI로 등록합니다. `registrationId`는 `google`, `kakao`, `naver` 중 하나입니다. `OAUTH2_REDIRECT_URI`는 로그인 완료 후 이동할 클라이언트 주소입니다.

</details>

### 3. 컨테이너 실행

```sh
docker compose up -d mysql redis
docker compose logs mysql
```

MySQL 로그에서 연결 준비가 완료된 것을 확인한 뒤 애플리케이션을 실행합니다.

```sh
docker compose up -d --build app
docker compose logs -f app
```

| 항목 | 접속 주소 |
| --- | --- |
| API 서버 | `http://localhost:8080` |
| 상태 확인 | [GET /health](http://localhost:8080/health) |
| Swagger UI | [API 문서 열기](http://localhost:8080/swagger-ui/index.html) |
| OpenAPI JSON | [API 명세 열기](http://localhost:8080/v3/api-docs) |
| MySQL 호스트 접속 | `localhost:3307` |
| Redis 호스트 접속 | `localhost:6379` |

Dockerfile은 Gradle 8.14·JDK 17로 빌드하고 테스트를 제외합니다. DB 테이블은 현재 `spring.jpa.hibernate.ddl-auto=update` 설정으로 생성·갱신됩니다.

### 4. 초기 데이터 적재

처음 실행한 DB에는 관광 데이터가 없으므로, 관광지 검색을 확인하려면 목록과 상세 정보를 적재해야 합니다. 회원가입·로그인 후 받은 Access Token을 `Authorization: Bearer <accessToken>` 헤더에 넣어 아래 순서로 요청합니다.

| 순서 | 요청 | 목적 |
| --- | --- | --- |
| 1 | `POST /api/regions/sync` | 지역·시군구 코드 수집 |
| 2 | `POST /admin/tour-batch/initial-load/ACCESSIBLE_LIST?maxPages=1` | 무장애 관광지 목록을 소량 적재 |
| 3 | `POST /admin/tour-batch/detail/accessible?batchSize=100` | 접근성 시설 정보 적재 |
| 4 | `POST /admin/tour-batch/detail/common-accessible?batchSize=100` | 관광지 공통 상세 정보 적재 |

목록 적재를 다시 요청하면 저장된 커서에서 이어서 처리합니다. 더 많은 데이터를 가져오려면 API 할당량에 맞춰 페이지 수와 배치 크기를 조절합니다. 접근성 필터 검색에는 3번 단계의 데이터가 필요합니다.

현재 설정의 정기 작업은 한국 시간 기준 변경분 동기화 매일 **21:45**, 원본에서 제거된 관광지 확인 매주 일요일 **05:00**입니다. 초기 전체 적재 스케줄은 기본 비활성화되어 있습니다.

<details>
<summary><strong>애플리케이션을 로컬 Gradle로 실행하기</strong></summary>

Java 17을 준비하고, MySQL·Redis 컨테이너만 실행합니다. `.env`의 DB 주소를 `jdbc:mysql://localhost:3307/modu?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul`, Redis 호스트를 `localhost`로 변경합니다.

로컬 실행에서는 `.env`를 Java properties 형식으로 읽으므로, Firebase 개인키의 각 줄바꿈을 `\\n`으로 기록해 이스케이프를 유지합니다. 운영체제 환경변수로 직접 전달할 때는 이 properties 이스케이프가 필요하지 않습니다.

`modu` 디렉터리에서 실행합니다.

```powershell
# Windows PowerShell
.\gradlew.bat bootRun
```

```sh
# macOS / Linux
sh ./gradlew bootRun
```

저장소의 Gradle Wrapper 배포 버전은 9.4.1이며 Docker 빌드 버전과 다릅니다.

</details>

## 주요 API

전체 요청·응답 구조는 서버 실행 후 Swagger UI에서 확인할 수 있습니다.

| 기능 | Method | Endpoint |
| --- | --- | --- |
| 회원가입 / 로그인 | `POST` | `/api/auth/signup`, `/api/auth/login` |
| 토큰 갱신 / 로그아웃 | `POST` | `/api/auth/refresh`, `/api/auth/logout` |
| 관광지 검색 / 상세 | `GET` | `/api/attractions`, `/api/attractions/{attractionId}` |
| 자연어 검색·상세 음성 안내 | `POST` | `/api/voice-search` |
| 일정 생성 / 목록 | `POST` / `GET` | `/api/schedules` |
| 일정 상세 | `GET` | `/api/schedules/{scheduleId}` |
| 일정에 관광지 추가 | `POST` | `/api/schedules/{scheduleId}/nodes` |
| 날짜·방문 순서 편집 | `PUT` | `/api/schedules/{scheduleId}/nodes/placement` |
| AI 자동 배치 | `POST` | `/api/schedules/{scheduleId}/auto-arrange` |
| 일정 안의 AI 명령 | `POST` | `/api/schedules/{scheduleId}/ai-command` |
| 일정 생성·검색을 포함한 AI 작업 | `POST` | `/api/ai/schedule-workflow` |
| 보호자 연결 요청 | `POST` | `/api/caregivers` |
| 도착 공유 설정 | `PATCH` | `/api/schedules/{scheduleId}/arrival-notification` |
| 도착 확인 | `POST` | `/api/schedules/{scheduleId}/nodes/{nodeId}/arrival` |
| 알림 조회 | `GET` | `/api/notifications` |
| 지역별 인기 관광지 | `GET` | `/api/regions/{regionCode}/popular-attractions` |

자연어 관광지 검색 요청 예시:

```http
POST /api/voice-search
Content-Type: application/json

{
  "text": "서울에서 휠체어를 이용할 수 있는 관광지를 찾아줘",
  "type": 1
}
```

이 요청은 문장에서 검색 조건을 추출한 뒤 DB를 조회하고, 관광지 목록과 해석된 필터를 함께 반환합니다. `type: 2`는 `attractionId`를 추가로 받아 관광지 상세 안내를 생성합니다.

## 테스트

저장소에는 관광지 검색·상세 조회, 일정 관리, 노드 배치, AI 재정렬 도구 관련 테스트 코드가 포함되어 있습니다.

| 대상 | 포함된 테스트 시나리오 |
| --- | --- |
| 관광지 검색 | 복수 접근성 조건, 접근성 상태, 지역·키워드·콘텐츠 유형 조건 |
| 관광지 상세 | 정상 조회와 존재하지 않는 관광지 처리 |
| 일정 관리 | 생성·조회·수정·삭제, 여행 기간·인원·예산 검증 |
| 노드 배치 | 미배치 상태, 날짜·순서 변경, 기간 밖 날짜, 중복 노드, 다른 일정의 노드 처리 |
| AI 도구 | 노드 재정렬 핸들러 통합 테스트 |

Java 17과 위 로컬 실행 환경을 준비한 뒤 `modu` 디렉터리에서 실행합니다. 전체 테스트에는 Spring 컨텍스트를 로드하는 테스트가 포함되어 있어 DB·Redis·Firebase 등의 설정이 필요합니다.

```powershell
# Windows PowerShell
.\gradlew.bat test
```

```sh
# macOS / Linux
sh ./gradlew test
```

테스트 결과 보고서는 `modu/build/reports/tests/test/index.html`에 생성됩니다. 위 표는 포함된 테스트 코드의 범위이며, 테스트 통과 결과나 커버리지 측정값을 의미하지 않습니다.

[auto-arrange-test.js](modu/auto-arrange-test.js)는 로그인부터 자동 배치 요청까지 실행하는 k6 스크립트입니다. 계정·일정 ID·여행 날짜를 실행 환경에 맞게 수정한 뒤 `k6 run auto-arrange-test.js`로 실행합니다. 현재 설정은 사용자 1명·요청 시나리오 1회이며, 별도의 부하 테스트 측정 결과는 포함되어 있지 않습니다.
