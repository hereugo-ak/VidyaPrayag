// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/core/PaginationParams.kt
package com.littlebridge.enrollplus.feature.pews.core

import io.ktor.server.application.ApplicationCall

/**
 * Shared pagination parameters extracted from query strings. Every PEWS module
 * that returns a list uses this so pagination is consistent across the API
 * (REUSABILITY mandate).
 *
 * Query params:
 *   page      — 1-based page number (default 1, min 1)
 *   page_size — items per page (default 20, min 1, max 100)
 *
 * Usage in a route:
 * ```
 * val params = PaginationParams.from(call)
 * val rows = repository.list(offset = params.offset, limit = params.limit)
 * ```
 *
 * SOLID:
 *   - S: one responsibility — pagination parameter extraction.
 *   - D: depends on Ktor's ApplicationCall, not on any PEWS module.
 */
data class PaginationParams(
    val page: Int = 1,
    val pageSize: Int = 20,
) {
    /** Zero-based offset for SQL LIMIT/OFFSET. */
    val offset: Int get() = ((page - 1) * pageSize).coerceAtLeast(0)

    /** Row count for SQL LIMIT. */
    val limit: Int get() = pageSize

    companion object {
        private const val DEFAULT_PAGE = 1
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 100

        /**
         * Extract pagination params from a Ktor call's query parameters.
         */
        fun from(call: ApplicationCall): PaginationParams {
            val page = call.request.queryParameters["page"]?.toIntOrNull()
                ?.coerceAtLeast(1) ?: DEFAULT_PAGE
            val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull()
                ?.coerceIn(1, MAX_PAGE_SIZE) ?: DEFAULT_PAGE_SIZE
            return PaginationParams(page = page, pageSize = pageSize)
        }
    }
}

/**
 * Generic paginated response wrapper for list endpoints.
 */
@kotlinx.serialization.Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val page: Int,
    val page_size: Int,
    val total: Int,
    val total_pages: Int,
) {
    companion object {
        fun <T> of(items: List<T>, params: PaginationParams, total: Int): PaginatedResponse<T> {
            val totalPages = if (total == 0) 0 else (total + params.pageSize - 1) / params.pageSize
            return PaginatedResponse(
                items = items,
                page = params.page,
                page_size = params.pageSize,
                total = total,
                total_pages = totalPages,
            )
        }
    }
}
