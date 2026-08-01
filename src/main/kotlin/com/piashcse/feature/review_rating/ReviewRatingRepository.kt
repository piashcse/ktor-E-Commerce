package com.piashcse.feature.review_rating

import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.model.response.ReviewRatingResponse
import com.piashcse.utils.common.PaginatedResponse

/**
 * Facts about a review used by the service layer to enforce authorization.
 */
data class ReviewAccess(
    val reviewId: String,
    val productId: String,
    val isOwner: Boolean,
)

/**
 * Facts used to authorize review creation: whether the user purchased the
 * product and whether the user is the product's seller.
 */
data class ReviewCreateAccess(
    val isVerifiedPurchase: Boolean,
    val isProductSeller: Boolean,
)

/**
 * Persistence boundary for the Review Rating aggregate.
 *
 * The repository is limited to data access (reads/writes + projection to DTOs).
 * All authorization, validation and transaction orchestration live in
 * [ReviewRatingService].
 */
interface ReviewRatingRepository {
    /**
     * Retrieves reviews and ratings for a product.
     */
    suspend fun getReviewRating(
        productId: String,
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<ReviewRatingResponse>

    /**
     * Resolves the authorization facts for a review relative to [userId].
     * Throws if the review does not exist.
     */
    suspend fun getReviewAccess(
        userId: String,
        reviewId: String,
    ): ReviewAccess

    /**
     * Resolves the facts needed to authorize review creation for a product.
     */
    suspend fun getReviewCreateAccess(
        userId: String,
        productId: String,
    ): ReviewCreateAccess

    /**
     * Persists a new review, rejecting duplicates for the same product by the
     * same user. Assumes the caller has validated the rating.
     */
    suspend fun addReviewRating(
        userId: String,
        reviewRating: ReviewRatingRequest,
    ): ReviewRatingResponse

    /**
     * Updates an existing review. Assumes the caller has authorized the review
     * and validated the rating.
     */
    suspend fun updateReviewRating(
        reviewId: String,
        review: String,
        rating: Int,
    ): ReviewRatingResponse

    /**
     * Deletes an existing review. Assumes the caller has authorized the review.
     */
    suspend fun deleteReviewRating(reviewId: String): String
}
