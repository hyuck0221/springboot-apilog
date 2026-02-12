package com.hshim.apilog.storage.http

import com.hshim.apilog.config.ApiLogProperties
import com.hshim.apilog.model.ApiLogEntry
import com.hshim.apilog.storage.ApiLogStorage
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * [ApiLogStorage] implementation that forwards every log entry to a remote HTTP endpoint
 * via `POST` with a JSON body.
 *
 * Designed to push logs from instrumented services to a central `apilog-view` server.
 *
 * **Target endpoint:** `apilog.storage.http.endpoint-url`
 * (e.g., `http://apilog-view:8080/apilog/logs/receive`)
 *
 * When `apilog.storage.http.async` is `true` (default) the HTTP call is made on a
 * background thread so it does not block the request thread. Failures are logged and
 * silently dropped — they do not propagate to the caller.
 */
class ApiLogHttpStorage(
    private val properties: ApiLogProperties,
    private val objectMapper: ObjectMapper,
) : ApiLogStorage {

    private val log = LoggerFactory.getLogger(ApiLogHttpStorage::class.java)
    private val httpProps = properties.storage.http

    private val executor = Executors.newFixedThreadPool(
        2,
        Thread.ofVirtual().factory(),   // Virtual threads (Java 21+) — lightweight
    )

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(httpProps.timeoutMs.toLong()))
        .build()

    override fun save(entry: ApiLogEntry) {
        if (httpProps.async) {
            executor.submit { doSend(entry) }
        } else {
            doSend(entry)
        }
    }

    private fun doSend(entry: ApiLogEntry) {
        try {
            val body = objectMapper.writeValueAsString(entry)
            val request = HttpRequest.newBuilder()
                .uri(URI.create(httpProps.endpointUrl))
                .header("Content-Type", "application/json; charset=UTF-8")
                .timeout(Duration.ofMillis(httpProps.timeoutMs.toLong()))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.discarding())
            if (response.statusCode() !in 200..299) {
                log.warn(
                    "ApiLog HTTP storage received non-2xx response: {} from {}",
                    response.statusCode(),
                    httpProps.endpointUrl,
                )
            }
        } catch (e: Exception) {
            log.error("Failed to POST log entry to ${httpProps.endpointUrl}", e)
        }
    }

    @PreDestroy
    fun shutdown() {
        executor.shutdown()
        runCatching { executor.awaitTermination(10, TimeUnit.SECONDS) }
    }
}
