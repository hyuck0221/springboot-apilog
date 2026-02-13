package com.hshim.apilog.document

/** Paginated list of [APIInfoResponse] records returned by the document API. */
data class ApiDocumentPage(
    val content: List<APIInfoResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
