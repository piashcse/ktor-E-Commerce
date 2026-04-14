package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val pagination: PaginationInfo
)

@Serializable
data class PaginationInfo(
    val page: Int,
    val limit: Int,
    val total: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrev: Boolean
) {
    companion object {
        fun from(page: Int, limit: Int, total: Long): PaginationInfo {
            val totalPages = ((total + limit - 1) / limit).toInt()
            return PaginationInfo(
                page = page,
                limit = limit,
                total = total,
                totalPages = totalPages,
                hasNext = page < totalPages,
                hasPrev = page > 1
            )
        }
    }
}
