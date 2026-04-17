package com.piashcse.utils

import kotlinx.serialization.Serializable

/**
 * Standardized pagination wrapper for all list endpoints.
 */
@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val metadata: PaginationMetadata
)

@Serializable
data class PaginationMetadata(
    val totalCount: Long,
    val limit: Int,
    val skip: Int,
    val nextCursor: String? = null
)
