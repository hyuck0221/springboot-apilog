package com.hshim.apilog.storage.local

import com.hshim.apilog.config.ApiLogProperties
import com.hshim.apilog.config.LogFileFormat
import com.hshim.apilog.model.ApiLogEntry
import com.hshim.apilog.storage.ApiLogStorage
import com.hshim.apilog.storage.file.ApiLogFileWriter
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import tools.jackson.databind.ObjectMapper
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * [ApiLogStorage] implementation that writes log entries to local files on disk.
 *
 * Log entries are buffered in memory and flushed to a new file whenever the buffer
 * reaches [ApiLogProperties.LocalFileStorageProperties.logsPerFile] entries.
 * Remaining buffered entries are also flushed on application shutdown via [@PreDestroy].
 *
 * Files are named `api_log_<timestamp>_<counter>.<ext>` and placed in the configured directory.
 * Format can be JSONL (one JSON object per line) or CSV.
 */
class ApiLogLocalFileStorage(
    private val properties: ApiLogProperties,
    private val objectMapper: ObjectMapper,
) : ApiLogStorage {

    private val log = LoggerFactory.getLogger(ApiLogLocalFileStorage::class.java)
    private val localProps = properties.storage.localFile

    private val buffer = mutableListOf<ApiLogEntry>()
    private val lock = ReentrantLock()
    private val fileCounter = AtomicInteger(0)

    @PostConstruct
    fun init() {
        val dir = File(localProps.path)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        log.info("ApiLog local file storage initialized. Directory: ${dir.absolutePath}")
    }

    override fun save(entry: ApiLogEntry) {
        lock.withLock {
            buffer.add(entry)
            if (buffer.size >= localProps.logsPerFile) {
                flushToFile(buffer.toList())
                buffer.clear()
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        lock.withLock {
            if (buffer.isNotEmpty()) {
                flushToFile(buffer.toList())
                buffer.clear()
            }
        }
    }

    private fun flushToFile(entries: List<ApiLogEntry>) {
        if (entries.isEmpty()) return

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"))
        val counter = fileCounter.getAndIncrement()
        val ext = if (localProps.format == LogFileFormat.CSV) "csv" else "jsonl"
        val file = File(localProps.path, "api_log_${timestamp}_$counter.$ext")

        try {
            file.bufferedWriter(Charsets.UTF_8).use { writer ->
                ApiLogFileWriter.writeEntries(
                    writer = writer,
                    entries = entries,
                    format = localProps.format,
                    objectMapper = objectMapper,
                    writeHeader = true,
                )
            }
            log.debug("Wrote {} log entries to {}", entries.size, file.absolutePath)
        } catch (e: Exception) {
            log.error("Failed to write log entries to file: ${file.absolutePath}", e)
        }
    }
}
