package com.hshim.apilog.storage.file

import com.hshim.apilog.config.LogFileFormat
import com.hshim.apilog.internal.ApiLogMapper
import com.hshim.apilog.model.ApiLogEntry
import java.io.BufferedWriter
import java.time.format.DateTimeFormatter

/**
 * Utility object that writes a batch of [ApiLogEntry] records to a [BufferedWriter]
 * in either JSONL or CSV format.
 */
internal object ApiLogFileWriter {

    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

    private val CSV_HEADERS = listOf(
        "id", "url", "method", "query_params", "request_headers", "request_body",
        "response_status", "response_content_type", "response_body",
        "request_time", "response_time", "processing_time_ms",
        "server_name", "server_port", "remote_addr",
    )

    fun writeEntries(
        writer: BufferedWriter,
        entries: List<ApiLogEntry>,
        format: LogFileFormat,
        writeHeader: Boolean = true,
    ) {
        when (format) {
            LogFileFormat.JSON -> writeJsonLines(writer, entries)
            LogFileFormat.CSV -> writeCsv(writer, entries, writeHeader)
        }
    }

    private fun writeJsonLines(
        writer: BufferedWriter,
        entries: List<ApiLogEntry>,
    ) {
        entries.forEach { entry ->
            writer.write(ApiLogMapper.toJson(entry))
            writer.newLine()
        }
    }

    private fun writeCsv(
        writer: BufferedWriter,
        entries: List<ApiLogEntry>,
        writeHeader: Boolean,
    ) {
        if (writeHeader) {
            writer.write(CSV_HEADERS.joinToString(","))
            writer.newLine()
        }
        entries.forEach { entry ->
            val row = listOf(
                entry.id.csvEscape(),
                entry.url.csvEscape(),
                entry.method.csvEscape(),
                ApiLogMapper.toJson(entry.queryParams).csvEscape(),
                ApiLogMapper.toJson(entry.requestHeaders).csvEscape(),
                (entry.requestBody ?: "").csvEscape(),
                entry.responseStatus.toString(),
                (entry.responseContentType ?: "").csvEscape(),
                (entry.responseBody ?: "").csvEscape(),
                entry.requestTime.format(DATE_FORMATTER),
                entry.responseTime.format(DATE_FORMATTER),
                entry.processingTimeMs.toString(),
                (entry.serverName ?: "").csvEscape(),
                (entry.serverPort?.toString() ?: ""),
                (entry.remoteAddr ?: "").csvEscape(),
            ).joinToString(",")
            writer.write(row)
            writer.newLine()
        }
    }

    private fun String.csvEscape(): String =
        if (contains(",") || contains("\"") || contains("\n") || contains("\r")) {
            "\"${replace("\"", "\"\"")}\""
        } else {
            this
        }
}
