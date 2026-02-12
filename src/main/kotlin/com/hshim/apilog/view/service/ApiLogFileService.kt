package com.hshim.apilog.view.service

import com.hshim.apilog.view.dto.ApiLogFileInfo
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException

/**
 * Service that lists and reads local log files written by [com.hshim.apilog.storage.local.ApiLogLocalFileStorage].
 *
 * Path-traversal protection: all file access is canonicalized and restricted to the requested directory.
 */
class ApiLogFileService {

    private val log = LoggerFactory.getLogger(ApiLogFileService::class.java)

    /**
     * Lists log files in [directory] sorted by last-modified descending.
     *
     * @param directory relative or absolute directory path (default: `logs/`)
     * @param maxFiles  maximum number of files to return (default: 5, max: 100)
     * @param format    file format filter — `"json"` matches `.jsonl` files, `"csv"` matches `.csv` files
     */
    fun listFiles(
        directory: String,
        maxFiles: Int,
        format: String,
    ): List<ApiLogFileInfo> {
        val safeMax = maxFiles.coerceIn(1, 100)
        val dir = File(directory).canonicalFile

        if (!dir.exists() || !dir.isDirectory) {
            log.debug("ApiLog file listing: directory not found or not a directory — {}", dir.absolutePath)
            return emptyList()
        }

        val extension = when (format.lowercase()) {
            "csv" -> ".csv"
            else  -> ".jsonl"
        }

        return dir.listFiles { f -> f.isFile && f.name.endsWith(extension) }
            ?.sortedByDescending { it.lastModified() }
            ?.take(safeMax)
            ?.map { f ->
                ApiLogFileInfo(
                    name         = f.name,
                    path         = f.path,
                    size         = f.length(),
                    lastModified = java.time.Instant.ofEpochMilli(f.lastModified()),
                )
            }
            ?: emptyList()
    }

    /**
     * Returns the raw text content of the file at [path].
     *
     * @throws IllegalArgumentException if the path is blank
     * @throws FileNotFoundException    if the file does not exist
     * @throws SecurityException        if the resolved path escapes [directory]
     */
    fun readFileContent(path: String, directory: String): String {
        require(path.isNotBlank()) { "path must not be blank" }

        val dir = File(directory).canonicalFile
        val target = File(path).canonicalFile

        // Path-traversal guard: resolved file must be inside the requested directory
        if (!target.absolutePath.startsWith(dir.absolutePath + File.separator) &&
            target.absolutePath != dir.absolutePath
        ) {
            throw SecurityException("Access denied: '$path' is outside the allowed directory '${dir.absolutePath}'")
        }

        if (!target.exists()) throw FileNotFoundException("File not found: ${target.absolutePath}")
        if (!target.isFile)   throw IllegalArgumentException("Not a file: ${target.absolutePath}")

        return target.readText(Charsets.UTF_8)
    }
}
