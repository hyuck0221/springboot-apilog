# apilog

모든 HTTP API 요청과 응답을 자동으로 기록하는 Spring Boot + Kotlin 라이브러리입니다.
**DB 테이블**, **Supabase DB**, **Supabase S3**, **로컬 파일** 저장 방식을 동시에 복수 선택하여 사용할 수 있습니다.

[English](./README.md)

---

## 요구 사항

| 구성 요소   | 버전   |
|-------------|--------|
| Spring Boot | 3.0+   |
| Kotlin      | 1.9+   |
| Java        | 17+    |

> **DB 저장**과 **View API**를 사용하려면 `spring-boot-starter-jdbc`와 `DataSource` 설정이 추가로 필요합니다.

---

## 설치

**Gradle (Kotlin DSL)**
```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.hyuck0221:apilog:0.0.1")
}
```

Spring Boot 자동 구성으로 활성화되므로 별도 어노테이션이 필요 없습니다.

---

## 기록 항목

| 필드                   | 설명                                                     |
|------------------------|----------------------------------------------------------|
| `id`                   | 로그 항목의 고유 UUID                                    |
| `appName`              | `apilog.app-name`으로 설정한 애플리케이션 이름           |
| `url`                  | 요청 URI 경로 (예: `/api/users`)                         |
| `method`               | HTTP 메서드 (GET, POST 등)                               |
| `queryParams`          | URL 쿼리 파라미터                                        |
| `requestHeaders`       | 요청 헤더 (민감 헤더는 마스킹 처리)                      |
| `requestBody`          | 요청 본문 텍스트                                         |
| `responseStatus`       | HTTP 응답 상태 코드                                      |
| `responseContentType`  | 응답 Content-Type 헤더                                   |
| `responseBody`         | 응답 본문 텍스트                                         |
| `requestTime`          | 요청이 수신된 시각                                       |
| `responseTime`         | 응답이 전송된 시각                                       |
| `processingTimeMs`     | 총 처리 시간 (밀리초)                                    |
| `serverName`           | 서버 호스트명                                            |
| `serverPort`           | 서버 포트                                                |
| `remoteAddr`           | 클라이언트 IP 주소                                       |

---

## 설정

```yaml
apilog:
  enabled: true                          # 전체 활성화 여부 (기본값: true)
  app-name: my-service                   # 모든 로그 항목에 포함될 애플리케이션 식별자

  exclude-paths:                         # 로깅에서 제외할 Ant 패턴 경로
    - /actuator/**
    - /swagger-ui/**

  mask-headers:                          # 값을 "***"으로 마스킹할 헤더 이름
    - Authorization
    - Cookie
    - Set-Cookie

  mask-request-body: false               # 요청 본문 전체를 "***"으로 대체
  mask-response-body: false              # 응답 본문 전체를 "***"으로 대체
  max-body-size: 10000                   # 본문 최대 기록 글자 수 (초과분 잘림)

  storage:
    db:
      enabled: false
      table-name: api_logs               # 테이블 이름 (기본값: api_logs)
      auto-create-table: true            # 시작 시 테이블 자동 생성 (기본값: true)

    local-file:
      enabled: false
      path: ./logs/api                   # 로그 파일 저장 디렉토리
      logs-per-file: 1000               # 파일 당 최대 로그 수 (기본값: 1000)
      format: JSON                       # JSON (JSONL) 또는 CSV

    supabase-s3:
      enabled: false
      endpoint-url: https://<ref>.supabase.co/storage/v1/s3
      access-key-id: <project-ref>
      secret-access-key: <service-role-key>
      region: ap-northeast-1
      bucket: api-logs
      key-prefix: logs/
      logs-per-file: 1000
      format: JSON

  view:
    enabled: false                       # View API 활성화 여부 (기본값: false)
    base-path: /apilog                   # View API 기본 경로
    api-key: ""                          # API 키 — 비워두면 인증 없이 접근 가능
    document:
      enabled: false                     # API 문서 엔드포인트 활성화 여부 (기본값: false)
```

---

## 저장 방식 상세

### DB 테이블

관계형 데이터베이스 테이블에 로그를 저장합니다.

- `spring-boot-starter-jdbc`와 `DataSource` 빈이 필요합니다.
- `auto-create-table: true`(기본값)이면 시작 시 `CREATE TABLE IF NOT EXISTS`로 테이블을 자동 생성합니다.
- H2, MySQL, PostgreSQL 등 대부분의 SQL 데이터베이스와 호환됩니다.

**테이블 스키마**
```sql
CREATE TABLE IF NOT EXISTS api_logs (
    id                    VARCHAR(36)  PRIMARY KEY,
    app_name              VARCHAR(255),
    url                   TEXT         NOT NULL,
    method                VARCHAR(10)  NOT NULL,
    query_params          TEXT,
    request_headers       TEXT,
    request_body          TEXT,
    response_status       INT,
    response_content_type VARCHAR(255),
    response_body         TEXT,
    request_time          TIMESTAMP    NOT NULL,
    response_time         TIMESTAMP    NOT NULL,
    processing_time_ms    BIGINT       NOT NULL,
    server_name           VARCHAR(255),
    server_port           INT,
    remote_addr           VARCHAR(255),
    created_at            TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
```

---

### 로컬 파일

로컬 디렉토리에 파일로 로그를 저장합니다.

- 로그 항목은 버퍼링되다가 `logs-per-file` 개수에 도달하면 새 파일로 플러시됩니다.
- 애플리케이션 종료 시 남은 버퍼도 자동 저장됩니다.
- 파일 이름 형식: `api_log_<yyyyMMdd_HHmmss_SSS>_<카운터>.<확장자>`

| 형식   | 확장자  | 설명                                |
|--------|---------|-------------------------------------|
| `JSON` | `.jsonl`| 줄 단위 JSON 객체 (JSONL 형식)      |
| `CSV`  | `.csv`  | 헤더 행이 포함된 CSV                |

---

### Supabase DB

앱의 `DataSource`와 무관한 **전용 JDBC 연결**로 Supabase PostgreSQL에 로그를 저장합니다.

- 컨슈머 프로젝트에 PostgreSQL JDBC 드라이버가 필요합니다:
  ```kotlin
  implementation("org.postgresql:postgresql")
  ```
- `CREATE TABLE IF NOT EXISTS`로 테이블 자동 생성 (`TIMESTAMPTZ` 타입 사용).
- 앱의 메인 DB 연결과 완전히 분리된 별도 연결을 사용합니다.

**인증 정보 확인 방법**

| 속성        | 확인 위치                                                              |
|-------------|------------------------------------------------------------------------|
| `jdbc-url`  | Supabase 대시보드 → 프로젝트 설정 → Database → Connection string (JDBC) |
| `username`  | 기본값: `postgres`                                                     |
| `password`  | Supabase 대시보드 → 프로젝트 설정 → Database → Database password        |

**직접 연결 vs. Connection Pooler**

```yaml
apilog:
  storage:
    supabase-db:
      enabled: true
      # 직접 연결 (포트 5432)
      jdbc-url: jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres
      # 또는 Connection Pooler 사용 (포트 6543, 서버리스 환경 권장)
      # jdbc-url: jdbc:postgresql://aws-0-<region>.pooler.supabase.com:6543/postgres
      username: postgres
      password: <db-password>
```

---

### Supabase S3

[Supabase Storage](https://supabase.com/docs/guides/storage)의 S3 호환 API를 통해 로그 파일을 업로드합니다.

- AWS SDK v2가 라이브러리에 포함되어 있어 별도 의존성이 필요 없습니다.
- 오브젝트 키: `<key-prefix>api_log_<yyyyMMdd_HHmmss_SSS>_<카운터>.<확장자>`

**인증 정보 확인 방법**

| 속성               | 확인 위치                                                          |
|--------------------|--------------------------------------------------------------------|
| `endpoint-url`     | `https://<project-ref>.supabase.co/storage/v1/s3`                 |
| `access-key-id`    | Supabase 대시보드 → 프로젝트 설정 → Storage → S3 액세스 키        |
| `secret-access-key`| S3 액세스 키 생성 시 발급                                          |
| `bucket`           | Supabase Storage에서 생성한 버킷 이름                              |

> **프로젝트 설정 → Storage → S3 연결**에서 S3 액세스를 먼저 활성화해야 합니다.

---

## View API (apilog-view 프론트엔드 제공 API)

`apilog-view` 서버 역할을 할 애플리케이션에 활성화합니다:

```yaml
apilog:
  storage:
    db:
      enabled: true                    # 필수 — 로그가 여기 저장되고 조회됩니다
  view:
    enabled: true
    base-path: /apilog
    api-key: your-secret-key          # 선택 — 비워두면 인증 없이 접근 가능
```

### API Key 인증

`apilog.view.api-key`에 값을 설정하면 View API의 **모든 요청**에 아래 헤더가 필요합니다:

```
X-Api-Key: your-secret-key
```

| 조건                              | 결과                          |
|-----------------------------------|-------------------------------|
| `api-key` 미설정 (기본값)         | 인증 없이 접근 가능           |
| `api-key` 설정 + 헤더 일치        | `200 OK` — 요청 통과          |
| `api-key` 설정 + 헤더 없거나 불일치 | `401 Unauthorized`           |

**401 응답 바디:**
```json
{"status": 401, "error": "Unauthorized", "message": "Invalid or missing X-Api-Key header"}
```

### 엔드포인트 목록

| 메서드 | 경로                        | 설명                                             |
|--------|-----------------------------|--------------------------------------------------|
| POST   | `/apilog/logs/receive`      | 원격 애플리케이션에서 로그 항목 수신             |
| GET    | `/apilog/logs`              | 페이지네이션 및 필터링된 로그 목록               |
| GET    | `/apilog/logs/{id}`         | 로그 항목 단건 상세 조회                         |
| GET    | `/apilog/logs/stats`        | 집계 통계                                        |
| GET    | `/apilog/logs/apps`         | 등록된 애플리케이션 이름 목록 (필터 드롭다운용)  |

### GET /apilog/logs — 쿼리 파라미터

| 파라미터             | 타입    | 설명                                                                     |
|----------------------|---------|--------------------------------------------------------------------------|
| `appName`            | String  | 애플리케이션 이름 완전 일치                                              |
| `method`             | String  | HTTP 메서드 (대소문자 무관)                                              |
| `url`                | String  | URL 경로; `%` 와일드카드 지원 (SQL LIKE)                                 |
| `statusCode`         | Int     | HTTP 응답 상태 코드                                                      |
| `startTime`          | String  | ISO-8601 날짜시간 — `requestTime >=` 필터 (예: `2026-01-15T00:00:00`)   |
| `endTime`            | String  | ISO-8601 날짜시간 — `requestTime <=` 필터                               |
| `minProcessingTimeMs`| Long    | 최소 처리 시간 (밀리초)                                                  |
| `page`               | Int     | 0부터 시작하는 페이지 번호 (기본값: `0`)                                 |
| `size`               | Int     | 페이지 크기 (기본값: `20`, 최대: `200`)                                  |
| `sortBy`             | String  | `request_time`, `processing_time_ms`, `response_status`, `url`, `method`, `app_name` |
| `sortDir`            | String  | `ASC` 또는 `DESC` (기본값: `DESC`)                                       |

### GET /apilog/logs/stats — 응답 예시

```json
{
  "totalCount": 3421,
  "countByMethod":  { "GET": 2100, "POST": 1200, "DELETE": 121 },
  "countByStatus":  { "200": 3000, "400": 300, "500": 121 },
  "countByAppName": { "order-service": 1800, "user-service": 1621 },
  "avgProcessingTimeMs": 42.7,
  "maxProcessingTimeMs": 3210,
  "p99ProcessingTimeMs": 890
}
```

---

## API 문서

이 애플리케이션에 등록된 **API 경로 목록을 검색·페이지네이션하여 제공**하는 기능입니다.
Swagger/OpenAPI 없이 간단한 API 브라우징이 필요할 때 활용할 수 있습니다.

### 요구 사항

- `apilog.view.enabled=true`
- `apilog.view.document.enabled=true`
- 클래스패스에 `spring-data-commons` 필요 (`spring-boot-starter-data-jpa` 등에 포함)

```yaml
apilog:
  view:
    enabled: true
    base-path: /apilog
    document:
      enabled: true
```

> 엔드포인트는 View API와 동일한 기본 경로를 사용하며, 동일한 `api-key` 인증이 적용됩니다.

### 엔드포인트 목록

| 메서드 | 경로                            | 설명                                 |
|--------|---------------------------------|--------------------------------------|
| GET    | `/apilog/document/status`       | API 문서 기능 활성화 여부 확인        |
| GET    | `/apilog/document`              | 페이지네이션 및 검색 가능한 API 목록  |

### GET /apilog/document — 쿼리 파라미터

| 파라미터   | 타입   | 설명                                                                       |
|------------|--------|----------------------------------------------------------------------------|
| `keyword`  | String | `url`, `title`, `description` 전체 텍스트 검색 (대소문자 무관)             |
| `category` | String | 카테고리 필터 (대소문자 무관 완전 일치)                                     |
| `method`   | String | HTTP 메서드 필터, 예: `GET`, `POST` (대소문자 무관)                        |
| `page`     | Int    | 0부터 시작하는 페이지 번호 (기본값: `0`)                                   |
| `size`     | Int    | 페이지 당 항목 수 (기본값: `20`)                                           |

### GET /apilog/document — 응답 예시

```json
{
  "content": [
    {
      "url": "/api/users",
      "method": "GET",
      "category": "User",
      "title": "사용자 목록 조회",
      "description": "페이지네이션된 사용자 목록을 반환합니다",
      "requestSchema": { "page": "int", "size": "int" },
      "responseSchema": { "id": "Long", "name": "String" },
      "requestInfos": [
        { "path": "page", "type": "int", "description": "페이지 번호", "nullable": true, "parameterType": "QUERY" }
      ],
      "responseInfos": [
        { "path": "id", "type": "Long", "description": "", "nullable": false, "parameterType": null }
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

> API 경로는 **애플리케이션 시작 시 한 번 스캔되어 캐싱**됩니다.
> `@Operation` (OpenAPI 3.x) 또는 `@ApiOperation` (Swagger 2.x) 어노테이션이 붙은 엔드포인트만 포함됩니다.

### GET /apilog/document/status — 응답

```json
{ "enabled": true }
```

`apilog.view.document.enabled=false`이면 `404 Not Found`를 반환합니다.

---

## 멀티 애플리케이션 아키텍처

View API의 `POST /apilog/logs/receive` 엔드포인트를 통해 다른 서비스에서 로그를 수집할 수 있습니다.
각 서비스에서 [커스텀 저장 백엔드](#커스텀-저장-백엔드)를 구현하여 중앙 서버로 로그를 전송하세요.

```
 ┌─────────────────┐  POST /apilog/logs/receive
 │  order-service  │─────────────────────────────────┐
 │  (커스텀 푸시)   │                                 │
 └─────────────────┘                                 ▼
                                           ┌──────────────────────┐
 ┌─────────────────┐  POST                 │  apilog-view 서버    │
 │  user-service   │──────────────────────▶│  (apilog 라이브러리  │
 │  (커스텀 푸시)   │                       │   + view.enabled=true)│
 └─────────────────┘                       └──────────┬───────────┘
                                                      │ GET /apilog/logs/**
                                                      ▼
                                           ┌──────────────────────┐
                                           │   apilog-view        │
                                           │   (프론트엔드)        │
                                           └──────────────────────┘
```

**apilog-view 서버** `application.yml`
```yaml
apilog:
  storage:
    db:
      enabled: true
  view:
    enabled: true
```

---

## 커스텀 저장 백엔드

`ApiLogStorage` 인터페이스를 구현하고 Spring 빈으로 등록하면 사용자 정의 저장소를 추가할 수 있습니다:

```kotlin
@Component
class MyCustomStorage : ApiLogStorage {
    override fun save(entry: ApiLogEntry) {
        // Kafka, Elasticsearch 등으로 전송
    }
}
```

`ApiLogStorage`를 구현한 모든 빈은 자동으로 감지되어 필터에서 사용됩니다.

---

## 라이브러리 비활성화

```yaml
apilog:
  enabled: false
```

---

## 라이선스

[Apache 2.0](LICENSE)
