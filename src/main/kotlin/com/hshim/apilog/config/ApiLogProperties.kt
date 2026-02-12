package com.hshim.apilog.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** File format used by local-file and S3 storage backends. */
enum class LogFileFormat {
    /** JSONL — one JSON object per line */
    JSON,

    /** CSV — header row followed by data rows */
    CSV,
}

@ConfigurationProperties(prefix = "apilog")
data class ApiLogProperties(

    /** Enable or disable the entire API logging filter. Default: true */
    val enabled: Boolean = true,

    /**
     * Logical name of this application.
     * Included in every log entry as `appName`.
     * Useful when multiple services send logs to a central apilog-view server.
     */
    val appName: String = "",

    /** Ant-style URL path patterns (wildcards supported) to exclude from logging. */
    val excludePaths: List<String> = emptyList(),

    /** Request header names whose values are replaced with "***" in the log. */
    val maskHeaders: List<String> = listOf("Authorization", "Cookie", "Set-Cookie"),

    /** Replace the entire request body with "***" instead of recording the actual content. */
    val maskRequestBody: Boolean = false,

    /** Replace the entire response body with "***" instead of recording the actual content. */
    val maskResponseBody: Boolean = false,

    /** Maximum number of characters recorded for request/response bodies. Excess is truncated. Default: 10000 */
    val maxBodySize: Int = 10000,

    val storage: StorageProperties = StorageProperties(),

    val view: ViewProperties = ViewProperties(),
) {

    data class StorageProperties(
        val db: DbStorageProperties = DbStorageProperties(),
        val localFile: LocalFileStorageProperties = LocalFileStorageProperties(),
        val supabaseDb: SupabaseDbStorageProperties = SupabaseDbStorageProperties(),
        val supabaseS3: SupabaseS3StorageProperties = SupabaseS3StorageProperties(),
        val http: HttpStorageProperties = HttpStorageProperties(),
    )

    // ── DB (app's own DataSource) ──────────────────────────────────────────────

    data class DbStorageProperties(
        /** Enable DB table storage using the application's configured DataSource. Default: false */
        val enabled: Boolean = false,

        /** Name of the table used to store API logs. Default: api_logs */
        val tableName: String = "api_logs",

        /** Automatically execute CREATE TABLE IF NOT EXISTS on startup. Default: true */
        val autoCreateTable: Boolean = true,
    )

    // ── Local file ────────────────────────────────────────────────────────────

    data class LocalFileStorageProperties(
        /** Enable local file storage. Default: false */
        val enabled: Boolean = false,

        /** Directory path where log files are written. Default: ./logs/api */
        val path: String = "./logs/api",

        /** Number of log entries buffered before a new file is flushed. Default: 1000 */
        val logsPerFile: Int = 1000,

        /** Output file format. JSON produces JSONL; CSV produces a CSV with a header row. Default: JSON */
        val format: LogFileFormat = LogFileFormat.JSON,
    )

    // ── Supabase DB ───────────────────────────────────────────────────────────

    data class SupabaseDbStorageProperties(
        /**
         * Enable Supabase PostgreSQL storage with a dedicated connection (independent of the app DataSource).
         * Default: false
         */
        val enabled: Boolean = false,

        /**
         * JDBC URL for the Supabase PostgreSQL database.
         * Direct:  jdbc:postgresql://db.[project-ref].supabase.co:5432/postgres
         * Pooler:  jdbc:postgresql://aws-0-[region].pooler.supabase.com:6543/postgres
         */
        val jdbcUrl: String = "",

        /** Supabase DB username. Default: postgres */
        val username: String = "postgres",

        /** Supabase DB password (found in Project Settings > Database). */
        val password: String = "",

        /** Name of the table used to store API logs. Default: api_logs */
        val tableName: String = "api_logs",

        /** Automatically execute CREATE TABLE IF NOT EXISTS on startup. Default: true */
        val autoCreateTable: Boolean = true,
    )

    // ── Supabase S3 ───────────────────────────────────────────────────────────

    data class SupabaseS3StorageProperties(
        /** Enable Supabase S3-compatible storage. Default: false */
        val enabled: Boolean = false,

        /**
         * Supabase S3-compatible endpoint URL.
         * Format: https://[project-ref].supabase.co/storage/v1/s3
         */
        val endpointUrl: String = "",

        /** S3 access key ID — found in Supabase Dashboard > Project Settings > Storage > S3 Access Keys. */
        val accessKeyId: String = "",

        /** S3 secret access key — generated when creating Supabase S3 access keys. */
        val secretAccessKey: String = "",

        /** AWS region string. Default: ap-northeast-1 */
        val region: String = "ap-northeast-1",

        /** Target bucket name in Supabase Storage. Default: api-logs */
        val bucket: String = "api-logs",

        /** S3 object key prefix (acts as a folder path). Default: logs/ */
        val keyPrefix: String = "logs/",

        /** Number of log entries buffered before uploading a new file. Default: 1000 */
        val logsPerFile: Int = 1000,

        /** Output file format. Default: JSON */
        val format: LogFileFormat = LogFileFormat.JSON,
    )

    // ── HTTP ──────────────────────────────────────────────────────────────────

    data class HttpStorageProperties(
        /**
         * Enable HTTP storage — posts each log entry to a remote endpoint (e.g., an apilog-view server).
         * Default: false
         */
        val enabled: Boolean = false,

        /**
         * URL of the remote endpoint that receives log entries.
         * Example: http://apilog-view:8080/apilog/logs/receive
         */
        val endpointUrl: String = "",

        /** HTTP connection and read timeout in milliseconds. Default: 5000 */
        val timeoutMs: Int = 5000,

        /**
         * Send log entries asynchronously (fire-and-forget).
         * When true the HTTP call does not block the request thread. Default: true
         */
        val async: Boolean = true,
    )

    // ── View API ──────────────────────────────────────────────────────────────

    data class ViewProperties(
        /**
         * Enable the built-in view API (log ingestion + query endpoints for the frontend).
         * Requires DB or Supabase DB storage to be enabled for the query endpoints.
         * Default: false
         */
        val enabled: Boolean = false,

        /** Base path for all view API endpoints. Default: /apilog */
        val basePath: String = "/apilog",
    )
}
