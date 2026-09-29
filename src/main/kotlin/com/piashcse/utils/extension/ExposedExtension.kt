package com.piashcse.utils.extension

import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.Query

/**
 * Returns the total count and paginated rows from a query.
 * Shared building block for paginated responses.
 */
fun <T> Query.toPaginatedList(
    limit: Int,
    offset: Int,
    mapper: (ResultRow) -> T,
): Pair<Long, List<T>> = count() to limit(limit).offset(offset.toLong()).map(mapper)

/**
 * Standardized extension for Exposed queries to return paginated results.
 */
fun <T> Query.toPaginatedResponse(
    limit: Int,
    offset: Int,
    mapper: (ResultRow) -> T,
): PaginatedResponse<T> {
    val (totalCount, data) = toPaginatedList(limit, offset, mapper)
    return PaginatedResponse(data, PaginationMetadata(totalCount, limit, offset))
}

/**
 * Paginate a query then preload related data before mapping.
 * Keeps results identical to manual toPaginatedList + preload + PaginatedResponse.
 */
fun <R, D> Query.paginateWithPreload(
    limit: Int,
    offset: Int,
    rowMapper: (ResultRow) -> R,
    preloadAndMap: (List<R>) -> List<D>,
): PaginatedResponse<D> {
    val (totalCount, rows) = toPaginatedList(limit, offset, rowMapper)
    return PaginatedResponse(preloadAndMap(rows), PaginationMetadata(totalCount, limit, offset))
}
