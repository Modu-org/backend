                               ## ✨ 변경 사항

* Tour API 연동을 위한 전체 구조 구현
* WebClient 기반 외부 API 호출 로직 추가
* 관광지 공통 정보 및 접근성 정보 조회 기능 구현

---

## 🔧 주요 구현 내용

### 1️⃣ API 호출 구조

**`TourApiClient`**

* WebClient 기반 GET 요청 처리
* JSON 응답을 `JsonNode`로 파싱
* 예외 처리 (응답 null, JSON 아님 등)

**`UriBuilderUtil`**

* 공통 query parameter 자동 추가
* `serviceKey`, `MobileOS`, `MobileApp` 포함

**`TourApiPath`**

* API endpoint 상수 관리

---

### 2️⃣ Service 레이어

**`TourApiService`**

* 공통 정보 조회 → `getCommon`
* 접근성 정보 조회 → `getAccessibility`
* 동기화 조회 → `getSync`
* 기본 파라미터 세팅 로직 분리

---

### 3️⃣ Controller (테스트용)

**`TourApiTestController`**

* `GET /api/test/tour/common/{contentId}`
* `GET /api/test/tour/accessibility/{contentId}`

---

### 4️⃣ 설정

**`TourApiProperties`**

* `application.yml` 값 바인딩

**`WebClientConfig`**

* WebClient Bean 등록

---

## 🧪 테스트 방법
* post man으로 테스트

```http
GET /api/test/tour/common/{contentId}
GET /api/test/tour/accessibility/{contentId}
```

---

## 📷 결과

---

## 🚧 향후 작업
