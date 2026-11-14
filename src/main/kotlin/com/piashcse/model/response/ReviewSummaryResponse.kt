package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ReviewSummaryResponse(
    val productId: String,
    val total: Int,
    val average: Double,
    /** One entry per star, 5 → 1, so the UI can draw the histogram without paging. */
    val buckets: List<RatingBucket>,
)

@Serializable
data class RatingBucket(
    val stars: Int,
    val count: Int,
)
