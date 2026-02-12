package com.hshim.apilog.storage.db

import com.hshim.apilog.config.ApiLogProperties
import com.hshim.apilog.model.ApiLogEntry
import com.hshim.apilog.storage.ApiLogStorage
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import tools.jackson.databind.ObjectMapper
import java.sql.Timestamp

/**
 * [ApiLogStorage] implementation that persists log entries into a relational database table
 * using Spring's [JdbcTemplate].
 *
 * The table is created automatically on startup if [ApiLogProperties.DbStorageProperties.autoCreateTable]
 * is `true` (default). Uses `CREATE TABLE IF NOT EXISTS` so it is safe to run against an existing table.
 *
 * Consumer applications must include `spring-boot-starter-jdbc` (or equivalent) on their classpath
 * and configure a `DataSource`.
 */
class ApiLogDbStorage(
    private val jdbcTemplate: JdbcTemplate,
    private val properties: ApiLogProperties,
    private val objectMapper: ObjectMapper,
) : ApiLogStorage {

    private val log = LoggerFactory.getLogger(ApiLogDbStorage::class.java)
    private val dbProps = properties.storage.db

    @PostConstruct
    fun init() {
        if (dbProps.autoCreateTable) {
            createTableIfNotExists()
        }
    }

    private fun createTableIfNotExists() {
        try {
            jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS ${dbProps.tableName} (
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
                )
                """.trimIndent(),
            )
            log.info("ApiLog DB table '${dbProps.tableName}' is ready.")
        } catch (e: Exception) {
            log.warn(
                "Could not create table '${dbProps.tableName}'. " +
                    "It may already exist or you may have insufficient permissions.",
                e,
            )
        }
    }

    override fun save(entry: ApiLogEntry) {
        jdbcTemplate.update(
            """
            INSERT INTO ${dbProps.tableName}
            (id, app_name, url, method, query_params, request_headers, request_body,
             response_status, response_content_type, response_body,
             request_time, response_time, processing_time_ms,
             server_name, server_port, remote_addr)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            entry.id,
            entry.appName,
            entry.url,
            entry.method,
            objectMapper.writeValueAsString(entry.queryParams),
            objectMapper.writeValueAsString(entry.requestHeaders),
            entry.requestBody,
            entry.responseStatus,
            entry.responseContentType,
            entry.responseBody,
            Timestamp.valueOf(entry.requestTime),
            Timestamp.valueOf(entry.responseTime),
            entry.processingTimeMs,
            entry.serverName,
            entry.serverPort,
            entry.remoteAddr,
        )
    }
}
