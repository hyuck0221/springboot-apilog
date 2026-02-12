package com.hshim.apilog.view.dto

import com.hshim.apilog.model.ApiLogEntry

/** Paginated list of [ApiLogEntry] records returned by the query API. */
data class ApiLogPage(
    val content: List<ApiLogEntry>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
