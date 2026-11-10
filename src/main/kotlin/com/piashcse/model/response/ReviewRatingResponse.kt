package com.piashcse.model.response

import com.piashcse.constants.ReviewStatus
import kotlinx.serialization.Serializable

@Serializable
data class ReviewRatingResponse(
    val id: String,
    val userId: String,
    val productId: String,
    val reviewText: String,
    val rating: Int,
    val title: String? = null,
    val isVerifiedPurchase: Boolean = false,
    val helpfulCount: Int = 0,
    val notHelpfulCount: Int = 0,
    val images: List<String> = emptyList(),
    val status: ReviewStatus = ReviewStatus.ACTIVE,
    val createdAt: String? = null,
)
