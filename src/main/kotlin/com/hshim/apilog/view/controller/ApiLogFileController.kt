package com.hshim.apilog.view.controller

import com.hshim.apilog.view.dto.ApiLogFileInfo
import com.hshim.apilog.view.service.ApiLogFileService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.FileNotFoundException

/**
 * REST controller that exposes local log files written by the local-file storage backend.
 *
 * Base path is shared with [ApiLogViewController] via `apilog.view.base-path` (default: `/apilog`).
 *
 * ## Endpoints
 *
 * | Method | Path                   | Description                          |
 * |--------|------------------------|--------------------------------------|
 * | GET    | `{base}/files`         | List log files in a directory        |
 * | GET    | `{base}/files/content` | Return raw text content of a file    |
 */
@RestController
@CrossOrigin
@RequestMapping("\${apilog.view.base-path:/apilog}")
class ApiLogFileController(
    private val fileService: ApiLogFileService, 
) {

    /**
     * Returns a list of log files in [directory], sorted by last-modified descending.
     *
     * @param directory relative or absolute path to the log directory (default: `logs/`)
     * @param maxFiles  maximum number of files to return (default: 5, max: 100)
     * @param format    file format to filter — `json` (→ `.jsonl`) or `csv` (→ `.csv`) (default: `json`)
     */
    @GetMapping("/files")
    fun listFiles(
        @RequestParam(defaultValue = "logs/") directory: String,
        @RequestParam(defaultValue = "5") maxFiles: Int,
        @RequestParam(defaultValue = "json") format: String,
    ): List<ApiLogFileInfo> = fileService.listFiles(directory, maxFiles, format)

    /**
     * Returns the raw text content of the file identified by [path].
     * The [path] value should be taken from the `path` field returned by `GET /files`.
     *
     * @param path      file path (as returned by the `/files` endpoint)
     * @param directory directory used as the security boundary for path-traversal protection
     *                  — must match the directory used when calling `/files` (default: `logs/`)
     */
    @GetMapping("/files/content")
    fun fileContent(
        @RequestParam path: String,
        @RequestParam(defaultValue = "logs/") directory: String,
    ): ResponseEntity<String> {
        return try {
            val content = fileService.readFileContent(path, directory)
            ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(content)
        } catch (e: FileNotFoundException) {
            ResponseEntity.notFound().build()
        } catch (e: SecurityException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.message)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message)
        }
    }
}
