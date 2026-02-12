package com.hshim.apilog.view.filter

import com.hshim.apilog.config.ApiLogProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Guards all View API endpoints with an API key check.
 *
 * Activated only when `apilog.view.api-key` is non-blank.
 * Every request must carry the header `X-Api-Key: <configured-key>`.
 * Missing or incorrect keys are rejected with **401 Unauthorized**.
 */
class ApiLogViewAuthFilter(
    private val properties: ApiLogProperties,
) : OncePerRequestFilter() {

    companion object {
        const val HEADER_NAME = "X-Api-Key"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val expectedKey = properties.view.apiKey
        val providedKey = request.getHeader(HEADER_NAME)

        if (providedKey != expectedKey) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json;charset=UTF-8"
            response.writer.write(
                """{"status":401,"error":"Unauthorized","message":"Invalid or missing $HEADER_NAME header"}""",
            )
            return
        }

        filterChain.doFilter(request, response)
    }
}
