package com.piashcse.utils.common

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val metadata: PaginationMetadata,
)

@Serializable
data class PaginationMetadata(
    val totalCount: Long,
    val limit: Int,
    val offset: Int,
    @EncodeDefault val page: Int = (offset / if (limit > 0) limit else 1) + 1,
    @EncodeDefault val perPage: Int = limit,
    @EncodeDefault
    val totalPages: Int = if (limit > 0) ((totalCount + limit.toLong() - 1) / limit.toLong()).toInt() else 0,
    @EncodeDefault val hasNext: Boolean = limit > 0 && offset + limit < totalCount,
    @EncodeDefault val hasPrev: Boolean = offset > 0,
) {
    val nextPage: Int? get() = if (hasNext) page + 1 else null
    val prevPage: Int? get() = if (hasPrev) page - 1 else null
}
