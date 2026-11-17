package com.piashcse.utils.common

/** Normalized offset pagination, shared by every route and repository. */
data class PageRequest(
    val limit: Int,
    val offset: Int = 0,
) {
    val page: Int get() = if (limit > 0) offset / limit + 1 else 1
    val perPage: Int get() = limit
}

/** Normalized keyset pagination, shared by cursor-based feeds. */
data class CursorRequest(
    val limit: Int,
    val cursor: String? = null,
)

/** Empty page with correct metadata for the requested window. */
fun <T> emptyPage(
    limit: Int,
    offset: Int = 0,
): PaginatedResponse<T> = PaginatedResponse(emptyList(), PaginationMetadata(0, limit, offset))
