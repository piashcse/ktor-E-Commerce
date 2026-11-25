package com.piashcse.feature.review_rating

import com.piashcse.constants.ReviewStatus
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class ReviewImagesRequest(val images: List<String>)

@Serializable
data class ReviewUpdateRequest(
    val review: String,
    val rating: Int,
)

@Serializable
data class ReviewModerationRequest(
    val status: ReviewStatus,
    val reason: String? = null,
)
