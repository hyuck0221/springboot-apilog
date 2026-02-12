package com.hshim.apilog.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a single API request/response log entry.
 */
data class ApiLogEntry(

    /** Unique identifier for this log entry. */
    val id: String = UUID.randomUUID().toString(),

    /** Name of the application that generated this log entry. Set via `apilog.app-name`. */
    val appName: String? = null,

    /** Request URI path (e.g., /api/users). */
    val url: String,

    /** HTTP method (GET, POST, PUT, DELETE, etc.). */
    val method: String,

    /** Query parameters from the request URL. */
    val queryParams: Map<String, List<String>> = emptyMap(),

    /** Request headers. Sensitive headers are masked based on configuration. */
    val requestHeaders: Map<String, String> = emptyMap(),

    /** Request body as a string. Null if empty or body logging is disabled. */
    val requestBody: String? = null,

    /** HTTP response status code. */
    val responseStatus: Int,

    /** Content-Type of the response. */
    val responseContentType: String? = null,

    /** Response body as a string. Null if empty or body logging is disabled. */
    val responseBody: String? = null,

    /** Timestamp when the request was received. */
    val requestTime: LocalDateTime,

    /** Timestamp when the response was sent. */
    val responseTime: LocalDateTime,

    /** Total processing time in milliseconds (responseTime - requestTime). */
    val processingTimeMs: Long,

    /** Server host name. */
    val serverName: String? = null,

    /** Server port. */
    val serverPort: Int? = null,

    /** Client remote IP address. */
    val remoteAddr: String? = null,
)
