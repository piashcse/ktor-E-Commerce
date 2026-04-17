package com.piashcse.utils.extension

import com.piashcse.utils.PaginatedResponse
import com.piashcse.utils.PaginationMetadata
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.Query
/**
 * Standardized extension for Exposed queries to return paginated results.
 * This encapsulates the limit, offset, and total count logic to ensure consistency
 * across all API endpoints and reduces boilerplate.
 *
 * @param limit The maximum number of items to return.
 * @param offset The starting index for the result set.
 * @param mapper A lambda to transform each ResultRow into a DTO or other model.
 */
fun <T> Query.toPaginatedResponse(
    limit: Int,
    offset: Int,
    mapper: (ResultRow) -> T
): PaginatedResponse<T> {
    val totalCount = this.count()
    val data = this.limit(limit).offset(offset.toLong()).map(mapper)

    return PaginatedResponse(
        data = data,
        metadata = PaginationMetadata(
            totalCount = totalCount,
            limit = limit,
            skip = offset
        )
    )
}
