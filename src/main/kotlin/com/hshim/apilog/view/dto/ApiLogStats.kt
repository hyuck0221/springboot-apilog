package com.hshim.apilog.view.dto

/** Aggregated statistics over a set of [com.hshim.apilog.model.ApiLogEntry] records. */
data class ApiLogStats(
    /** Total number of log entries matching the query. */
    val totalCount: Long,

    /** Entry count grouped by HTTP method (e.g., GET → 120, POST → 45). */
    val countByMethod: Map<String, Long>,

    /** Entry count grouped by HTTP response status code (e.g., 200 → 150, 404 → 10). */
    val countByStatus: Map<Int, Long>,

    /** Entry count grouped by application name. */
    val countByAppName: Map<String, Long>,

    /** Average processing time in milliseconds. */
    val avgProcessingTimeMs: Double,

    /** Maximum processing time in milliseconds. */
    val maxProcessingTimeMs: Long,

    /** 99th-percentile processing time in milliseconds. Null when insufficient data. */
    val p99ProcessingTimeMs: Long?,
)
