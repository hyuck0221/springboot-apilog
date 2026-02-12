package com.hshim.apilog.view.dto

/**
 * Query parameters used to filter and paginate the log list.
 * All filter fields are optional — omitting them returns all records.
 */
data class ApiLogQuery(
    /** Filter by application name (exact match). */
    val appName: String? = null,

    /** Filter by HTTP method (case-insensitive, e.g., GET, POST). */
    val method: String? = null,

    /** Filter by URL path — supports `%` wildcard (SQL LIKE). */
    val url: String? = null,

    /** Filter by HTTP response status code. */
    val statusCode: Int? = null,

    /**
     * Filter entries with `requestTime` on or after this timestamp.
     * ISO-8601 format: `2026-01-15T00:00:00`
     */
    val startTime: String? = null,

    /**
     * Filter entries with `requestTime` before or at this timestamp.
     * ISO-8601 format: `2026-01-15T23:59:59`
     */
    val endTime: String? = null,

    /** Filter entries where processing time is at least this many milliseconds. */
    val minProcessingTimeMs: Long? = null,

    /** 0-based page number. Default: 0 */
    val page: Int = 0,

    /** Number of entries per page. Default: 20, max: 200 */
    val size: Int = 20,

    /**
     * Sort field. Allowed values: `request_time`, `processing_time_ms`, `response_status`, `url`, `method`.
     * Default: `request_time`
     */
    val sortBy: String = "request_time",

    /** Sort direction: `ASC` or `DESC`. Default: `DESC` */
    val sortDir: String = "DESC",
)
