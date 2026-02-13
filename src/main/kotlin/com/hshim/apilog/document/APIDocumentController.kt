package com.hshim.apilog.document

import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller that exposes this application's registered API routes as
 * a searchable, paginated document.
 *
 * Base path: `{apilog.view.base-path}/document` (default: `/apilog/document`)
 *
 * ## Endpoints
 *
 * | Method | Path                            | Description                              |
 * |--------|---------------------------------|------------------------------------------|
 * | GET    | `{base}/document/status`        | Check whether the document API is active |
 * | GET    | `{base}/document`               | Paginated & searchable API list          |
 *
 * Activated when `apilog.view.enabled=true` and `apilog.view.document.enabled=true`.
 * Requires `spring-data-commons` on the classpath (for Pageable/Page support).
 */
@RestController
@CrossOrigin
@RequestMapping("\${apilog.view.base-path:/apilog}/document")
class APIDocumentController(
    private val documentComponent: APIDocumentComponent,
) {

    /**
     * Returns `{"enabled": true}` to indicate the document API is active.
     * When the feature is disabled this endpoint does not exist (404).
     */
    @GetMapping("/status")
    fun status(): Map<String, Boolean> = mapOf("enabled" to true)

    /**
     * Returns a paginated list of API routes registered in this application.
     *
     * All filter parameters are optional and combined with AND logic.
     *
     * @param keyword  Full-text search across `url`, `title`, and `description` (case-insensitive)
     * @param category Filter by category (case-insensitive exact match)
     * @param method   Filter by HTTP method, e.g. `GET`, `POST` (case-insensitive)
     * @param page     0-based page number (default: 0)
     * @param size     Number of items per page (default: 20)
     */
    @GetMapping
    fun list(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) method: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiDocumentPage {
        val all = documentComponent.getAPIInfos()

        val filtered = all.filter { api ->
            val matchKeyword = keyword.isNullOrBlank() ||
                api.url.contains(keyword, ignoreCase = true) ||
                api.title.contains(keyword, ignoreCase = true) ||
                api.description.contains(keyword, ignoreCase = true)
            val matchCategory = category.isNullOrBlank() ||
                api.category.equals(category, ignoreCase = true)
            val matchMethod = method.isNullOrBlank() ||
                api.method.equals(method, ignoreCase = true)
            matchKeyword && matchCategory && matchMethod
        }

        val totalElements = filtered.size.toLong()
        val safeSize = if (size <= 0) 20 else size
        val totalPages = ((totalElements + safeSize - 1) / safeSize).toInt().coerceAtLeast(1)
        val fromIndex = page * safeSize
        val content = if (fromIndex >= filtered.size) {
            emptyList()
        } else {
            filtered.subList(fromIndex, minOf(fromIndex + safeSize, filtered.size))
        }

        return ApiDocumentPage(
            content = content,
            page = page,
            size = safeSize,
            totalElements = totalElements,
            totalPages = totalPages,
        )
    }
}
