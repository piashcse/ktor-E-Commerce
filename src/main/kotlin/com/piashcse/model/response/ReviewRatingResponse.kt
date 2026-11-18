package com.piashcse.model.response

import com.piashcse.constants.ReviewStatus
import kotlinx.serialization.Serializable

@Serializable
data class ReviewRatingResponse(
    val id: String,
    /** Hidden on public review feeds to avoid publishing reviewer identifiers. */
    val userId: String? = null,
    val productId: String,
    val reviewText: String,
    val rating: Int,
    val title: String? = null,
    val isVerifiedPurchase: Boolean = false,
    val helpfulCount: Int = 0,
    val notHelpfulCount: Int = 0,
    val images: List<String> = emptyList(),
    val status: ReviewStatus = ReviewStatus.ACTIVE,
    val sellerReply: String? = null,
    val sellerRepliedAt: String? = null,
    val moderationReason: String? = null,
    val createdAt: String? = null,
)
