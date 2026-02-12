package com.hshim.apilog.view.dto

import java.time.Instant

/** Metadata for a single log file returned by the `GET {basePath}/files` endpoint. */
data class ApiLogFileInfo(
    /** File name (e.g. `api_log_20240115_123456_0.jsonl`). */
    val name: String,

    /** Relative path to use with `GET {basePath}/files/content?path=<value>`. */
    val path: String,

    /** File size in bytes. */
    val size: Long,

    /** Last-modified timestamp (ISO-8601 UTC). */
    val lastModified: Instant,
)
