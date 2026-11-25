package com.piashcse.feature.seller

import com.piashcse.constants.DisputeStatus
import com.piashcse.constants.DisputeType
import com.piashcse.constants.ReviewStatus
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import kotlinx.serialization.Serializable

@Serializable
data class SellerReviewRequest(
    val orderId: String,
    val rating: Int,
    val comment: String? = null,
) {
    init {
        if (rating !in 1..5) throw FieldValidationException(listOf(FieldViolation("rating", "Rating must be 1..5")))
    }
}

@Serializable
data class SellerReviewStatusRequest(
    val status: ReviewStatus,
    val reason: String? = null,
)

@Serializable
data class SellerReviewResponse(
    val id: String,
    val sellerId: String,
    val shopId: String?,
    val userId: String? = null,
    val orderId: String? = null,
    val rating: Int,
    val comment: String?,
    val status: ReviewStatus,
    val sellerReply: String? = null,
    val sellerRepliedAt: String? = null,
    val moderationReason: String? = null,
    val createdAt: String?,
)

@Serializable
data class DisputeRequest(
    val orderId: String? = null,
    val sellerId: String? = null,
    val type: DisputeType,
    val subject: String,
    val description: String,
) {
    init {
        if (subject.isBlank()) throw FieldValidationException(listOf(FieldViolation("subject", "Subject is required")))
        if (description.isBlank()) {
            throw FieldValidationException(listOf(FieldViolation("description", "Description is required")))
        }
    }
}

@Serializable
data class DisputeUpdateRequest(
    val status: DisputeStatus,
    val resolution: String? = null,
)

@Serializable
data class DisputeMessageRequest(
    val body: String,
    val isInternal: Boolean = false,
)

@Serializable
data class DisputeMessageResponse(
    val id: String,
    val authorId: String,
    val body: String,
    val isInternal: Boolean,
    val createdAt: String?,
)

@Serializable
data class DisputeDetailResponse(
    val dispute: DisputeResponse,
    val messages: List<DisputeMessageResponse>,
)

@Serializable
data class DisputeResponse(
    val id: String,
    val orderId: String?,
    val sellerId: String?,
    val userId: String,
    val type: DisputeType,
    val subject: String,
    val description: String,
    val status: DisputeStatus,
    val resolution: String?,
    val resolvedBy: String?,
    val resolvedAt: String?,
    val createdAt: String?,
)

@Serializable
data class SellerReviewReplyRequest(val reply: String)
