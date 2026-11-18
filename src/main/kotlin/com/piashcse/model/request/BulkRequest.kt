package com.piashcse.model.request

import kotlinx.serialization.Serializable

/** Shared bulk-selection payload for admin queues. */
@Serializable
data class BulkIdsRequest(
    val ids: List<String>,
    val reason: String? = null,
)

@Serializable
data class BulkResultResponse(
    val requested: Int,
    val succeeded: Int,
    val failed: List<String> = emptyList(),
) {
    companion object {
        fun of(
            requested: Int,
            failed: List<String>,
        ) = BulkResultResponse(requested, requested - failed.size, failed)
    }
}
