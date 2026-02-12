# apilog

A Spring Boot + Kotlin library that automatically records every HTTP API request and response.
Supports multiple storage backends simultaneously: **DB table**, **Supabase S3**, **local file**, and **HTTP push** to a central `apilog-view` server.

[한국어](./README_KO.md)

---

## Requirements

| Component    | Version |
|--------------|---------|
| Spring Boot  | 4.0+    |
| Kotlin       | 2.0+    |
| Java         | 21+     |

> **DB storage** and **View API** additionally require `spring-boot-starter-jdbc` and a configured `DataSource`.

---

## Installation

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

Activated automatically via Spring Boot auto-configuration — no extra annotation needed.

---

## What Is Recorded

| Field                  | Description                                       |
|------------------------|---------------------------------------------------|
| `id`                   | Unique UUID for the log entry                     |
| `appName`              | Application name set via `apilog.app-name`        |
| `url`                  | Request URI path (e.g., `/api/users`)             |
| `method`               | HTTP method (GET, POST, …)                        |
| `queryParams`          | URL query parameters                              |
| `requestHeaders`       | Request headers (sensitive ones are masked)       |
| `requestBody`          | Request body text                                 |
| `responseStatus`       | HTTP response status code                         |
| `responseContentType`  | Response Content-Type header                      |
| `responseBody`         | Response body text                                |
| `requestTime`          | Timestamp when the request was received           |
| `responseTime`         | Timestamp when the response was sent              |
| `processingTimeMs`     | Processing time in milliseconds                   |
| `serverName`           | Server host name                                  |
| `serverPort`           | Server port                                       |
| `remoteAddr`           | Client IP address                                 |

---

## Configuration

```yaml
apilog:
  enabled: true                          # Master switch (default: true)
  app-name: my-service                   # Application identifier included in every log entry

  exclude-paths:                         # Ant-style paths to skip
    - /actuator/**
    - /swagger-ui/**

  mask-headers:                          # Headers replaced with "***"
    - Authorization
    - Cookie
    - Set-Cookie

  mask-request-body: false               # Replace entire request body with "***"
  mask-response-body: false              # Replace entire response body with "***"
  max-body-size: 10000                   # Max characters per body (excess truncated)

  storage:
    db:
      enabled: false
      table-name: api_logs               # Table name (default: api_logs)
      auto-create-table: true            # Auto-create table on startup (default: true)

    local-file:
      enabled: false
      path: ./logs/api                   # Output directory
      logs-per-file: 1000               # Entries per file (default: 1000)
      format: JSON                       # JSON (JSONL) or CSV

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

    http:
      enabled: false
      endpoint-url: http://apilog-view:8080/apilog/logs/receive
      timeout-ms: 5000                   # Connection + read timeout (default: 5000 ms)
      async: true                        # Fire-and-forget, non-blocking (default: true)

  view:
    enabled: false                       # Enable the built-in View API (default: false)
    base-path: /apilog                   # Base path for all View API endpoints
```

---

## Storage Options

### DB Table

Persists log entries to a relational database.

- Requires `spring-boot-starter-jdbc` and a `DataSource` bean.
- Table is created automatically with `CREATE TABLE IF NOT EXISTS`.
- Compatible with H2, MySQL, PostgreSQL, and most SQL databases.

**Table schema**
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

### Local File

Writes log entries to files on disk.

- Entries are buffered and flushed to a new file when the buffer reaches `logs-per-file`.
- Remaining buffer is flushed on application shutdown.
- File naming: `api_log_<yyyyMMdd_HHmmss_SSS>_<counter>.<ext>`

| Format | Extension | Description                      |
|--------|-----------|----------------------------------|
| `JSON` | `.jsonl`  | One JSON object per line (JSONL) |
| `CSV`  | `.csv`    | CSV with a header row            |

---

### Supabase DB

Persists log entries to a **Supabase PostgreSQL** database using a dedicated JDBC connection,
independent of the application's own `DataSource`.

- Requires the PostgreSQL JDBC driver on the consumer's classpath:
  ```kotlin
  implementation("org.postgresql:postgresql")
  ```
- Table is created automatically with `CREATE TABLE IF NOT EXISTS` (uses `TIMESTAMPTZ`).
- Connection is managed separately from the app's main database.

**Finding your credentials**

| Property    | Where to find it                                                  |
|-------------|-------------------------------------------------------------------|
| `jdbc-url`  | Supabase Dashboard → Project Settings → Database → Connection string (JDBC) |
| `username`  | Default: `postgres`                                               |
| `password`  | Supabase Dashboard → Project Settings → Database → Database password |

**Direct vs. Connection Pooler**

```yaml
apilog:
  storage:
    supabase-db:
      enabled: true
      # Direct connection (port 5432)
      jdbc-url: jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres
      # Or use the connection pooler (port 6543, recommended for serverless)
      # jdbc-url: jdbc:postgresql://aws-0-<region>.pooler.supabase.com:6543/postgres
      username: postgres
      password: <db-password>
```

---

### Supabase S3

Uploads log files to [Supabase Storage](https://supabase.com/docs/guides/storage) via the S3-compatible API.

- AWS SDK v2 S3 client is included in the library (no extra dependency needed).
- Object key: `<key-prefix>api_log_<yyyyMMdd_HHmmss_SSS>_<counter>.<ext>`

**Finding your credentials**

| Property           | Where to find it                                           |
|--------------------|------------------------------------------------------------|
| `endpoint-url`     | `https://<project-ref>.supabase.co/storage/v1/s3`         |
| `access-key-id`    | Supabase Dashboard → Project Settings → Storage → S3 Keys |
| `secret-access-key`| Generated when creating S3 access keys                    |
| `bucket`           | The bucket name in Supabase Storage                        |

> Enable S3 access in **Supabase Dashboard → Project Settings → Storage → S3 Connection**.

---

### HTTP (Push to apilog-view)

Posts each log entry to a remote HTTP endpoint via JSON `POST`.

- Designed to push logs from instrumented services to a central `apilog-view` server.
- `async: true` (default) sends on a background virtual thread — request latency is not affected.
- Failures are logged and silently dropped; they never propagate to the caller.

---

## View API (for apilog-view frontend)

Enable on the **central log collection server** that `apilog-view` talks to:

```yaml
apilog:
  app-name: apilog-view-server
  storage:
    db:
      enabled: true                    # Required — logs are stored here and queried
  view:
    enabled: true
    base-path: /apilog
```

### Endpoints

| Method | Path                        | Description                                      |
|--------|-----------------------------|--------------------------------------------------|
| POST   | `/apilog/logs/receive`      | Ingest a log entry from a remote application     |
| GET    | `/apilog/logs`              | Paginated & filtered list of log entries         |
| GET    | `/apilog/logs/{id}`         | Single log entry detail                          |
| GET    | `/apilog/logs/stats`        | Aggregated statistics                            |
| GET    | `/apilog/logs/apps`         | Distinct application names (for filter dropdown) |

### GET /apilog/logs — Query Parameters

| Parameter            | Type    | Description                                                          |
|----------------------|---------|----------------------------------------------------------------------|
| `appName`            | String  | Exact match on application name                                      |
| `method`             | String  | HTTP method (case-insensitive)                                       |
| `url`                | String  | URL path; supports `%` wildcard (SQL LIKE)                           |
| `statusCode`         | Int     | HTTP response status code                                            |
| `startTime`          | String  | ISO-8601 datetime — filter `requestTime >=` this value              |
| `endTime`            | String  | ISO-8601 datetime — filter `requestTime <=` this value              |
| `minProcessingTimeMs`| Long    | Minimum processing time in ms                                        |
| `page`               | Int     | 0-based page number (default: `0`)                                   |
| `size`               | Int     | Page size (default: `20`, max: `200`)                                |
| `sortBy`             | String  | `request_time`, `processing_time_ms`, `response_status`, `url`, `method`, `app_name` |
| `sortDir`            | String  | `ASC` or `DESC` (default: `DESC`)                                    |

### GET /apilog/logs/stats — Response

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

## Multi-Application Architecture

```
 ┌─────────────────┐  HTTP POST /apilog/logs/receive
 │  order-service  │─────────────────────────────────┐
 │  (apilog lib)   │                                 │
 └─────────────────┘                                 ▼
                                           ┌──────────────────────┐
 ┌─────────────────┐  HTTP POST            │   apilog-view server │
 │  user-service   │──────────────────────▶│   (apilog lib +      │
 │  (apilog lib)   │                       │    view.enabled=true) │
 └─────────────────┘                       └──────────┬───────────┘
                                                      │ GET /apilog/logs/**
                                                      ▼
                                           ┌──────────────────────┐
                                           │   apilog-view        │
                                           │   (frontend)         │
                                           └──────────────────────┘
```

**order-service / user-service** `application.yml`
```yaml
apilog:
  app-name: order-service   # or user-service
  storage:
    http:
      enabled: true
      endpoint-url: http://apilog-view-server:8080/apilog/logs/receive
```

**apilog-view server** `application.yml`
```yaml
apilog:
  storage:
    db:
      enabled: true
  view:
    enabled: true
```

---

## Custom Storage Backend

Implement `ApiLogStorage` and register it as a Spring bean:

```kotlin
@Component
class MyCustomStorage : ApiLogStorage {
    override fun save(entry: ApiLogEntry) {
        // e.g., send to Kafka, Elasticsearch, etc.
    }
}
```

All `ApiLogStorage` beans are automatically discovered and used by the filter.

---

## Disable the Library

```yaml
apilog:
  enabled: false
```

---

## License

[Apache 2.0](LICENSE)
