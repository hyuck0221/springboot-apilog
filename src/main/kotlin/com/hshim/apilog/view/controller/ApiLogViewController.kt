package com.hshim.apilog.view.controller

import com.hshim.apilog.model.ApiLogEntry
import com.hshim.apilog.storage.ApiLogStorage
import com.hshim.apilog.view.dto.ApiLogPage
import com.hshim.apilog.view.dto.ApiLogQuery
import com.hshim.apilog.view.dto.ApiLogStats
import com.hshim.apilog.view.service.ApiLogViewService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller that provides the `apilog-view` frontend with log data.
 *
 * Base path is configurable via `apilog.view.base-path` (default: `/apilog`).
 *
 * ## Endpoints
 *
 * | Method | Path                          | Description                                    |
 * |--------|-------------------------------|------------------------------------------------|
 * | POST   | `{base}/logs/receive`         | Ingest a log entry from another application    |
 * | GET    | `{base}/logs`                 | Paginated & filtered list of log entries       |
 * | GET    | `{base}/logs/{id}`            | Single log entry detail                        |
 * | GET    | `{base}/logs/stats`           | Aggregated statistics                          |
 * | GET    | `{base}/logs/apps`            | Distinct application names                     |
 */
@RestController
@CrossOrigin
@RequestMapping("\${apilog.view.base-path:/apilog}")
class ApiLogViewController(
    private val viewService: ApiLogViewService,
    private val storagesProvider: ObjectProvider<ApiLogStorage>,
) {

    // ── Ingestion ─────────────────────────────────────────────────────────────

    @PostMapping("/logs/receive")
    fun receive(@RequestBody entry: ApiLogEntry): ResponseEntity<Void> {
        storagesProvider.orderedStream().forEach { it.save(entry) }
        return ResponseEntity.accepted().build()
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    @GetMapping("/logs")
    fun list(
        @RequestParam(required = false) appName: String?,
        @RequestParam(required = false) method: String?,
        @RequestParam(required = false) url: String?,
        @RequestParam(required = false) statusCode: String?,
        @RequestParam(required = false) startTime: String?,
        @RequestParam(required = false) endTime: String?,
        @RequestParam(required = false) minProcessingTimeMs: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "request_time") sortBy: String,
        @RequestParam(defaultValue = "DESC") sortDir: String,
    ): ApiLogPage = viewService.queryLogs(
        ApiLogQuery(
            appName = appName,
            method = method,
            url = url,
            statusCode = statusCode,
            startTime = startTime,
            endTime = endTime,
            minProcessingTimeMs = minProcessingTimeMs,
            page = page,
            size = size,
            sortBy = sortBy,
            sortDir = sortDir,
        ),
    )

    @GetMapping("/logs/{id}")
    fun getById(@PathVariable id: String): ResponseEntity<ApiLogEntry> {
        val entry = viewService.findById(id)
        return if (entry != null) ResponseEntity.ok(entry) else ResponseEntity.notFound().build()
    }

    @GetMapping("/logs/stats")
    fun stats(
        @RequestParam(required = false) startTime: String?,
        @RequestParam(required = false) endTime: String?,
    ): ApiLogStats = viewService.stats(startTime, endTime)

    @GetMapping("/logs/apps")
    fun listApps(): List<String> = viewService.listApps()
}
