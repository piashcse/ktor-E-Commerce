package com.piashcse.feature.review_rating

import com.piashcse.constants.CacheKeys
import com.piashcse.constants.Message
import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.model.response.ReviewRatingResponse
import com.piashcse.service.Cache
import com.piashcse.service.CacheService
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.suspendRetryQuery
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException

class ReviewRatingService(
    private val reviewRatingRepo: ReviewRatingRepository,
    private val cache: Cache = CacheService.cache,
) {
    suspend fun getReviewRating(
        productId: String,
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<ReviewRatingResponse> = reviewRatingRepo.getReviewRating(productId, limit, offset)

    /**
     * Adds a review, enforcing the rating range, verified-purchase and
     * no-self-review rules. Runs in a retryable transaction.
     */
    suspend fun addReviewRating(
        userId: String,
        reviewRating: ReviewRatingRequest,
    ): ReviewRatingResponse = suspendRetryQuery {
        if (reviewRating.rating < 1 || reviewRating.rating > 5)
            throw ValidationException(Message.Validation.RATING_OUT_OF_RANGE)

        val access = reviewRatingRepo.getReviewCreateAccess(userId, reviewRating.productId)
        if (access.isProductSeller) throw ForbiddenException(Message.Validation.SELLER_SELF_REVIEW_FORBIDDEN)
        if (!access.isVerifiedPurchase) throw ValidationException(Message.Validation.VERIFIED_PURCHASE_REQUIRED)

        reviewRatingRepo.addReviewRating(userId, reviewRating)
    }.also { cache.invalidatePattern(CacheKeys.PRODUCTS_PATTERN) }

    /**
     * Updates a review, enforcing the rating range rule and ownership.
     * Runs in a retryable transaction.
     */
    suspend fun updateReviewRating(
        userId: String,
        reviewId: String,
        review: String,
        rating: Int,
    ): ReviewRatingResponse = suspendRetryQuery {
        if (rating < 1 || rating > 5)
            throw ValidationException(Message.Validation.RATING_OUT_OF_RANGE)
        val access = reviewRatingRepo.getReviewAccess(userId, reviewId)
        if (!access.isOwner) throw ForbiddenException(Message.Errors.notOwner("review"))
        reviewRatingRepo.updateReviewRating(reviewId, review, rating)
    }.also { cache.invalidatePattern(CacheKeys.PRODUCTS_PATTERN) }

    /**
     * Deletes a review after verifying ownership. Runs in a retryable transaction.
     */
    suspend fun deleteReviewRating(userId: String, reviewId: String): String = suspendRetryQuery {
        val access = reviewRatingRepo.getReviewAccess(userId, reviewId)
        if (!access.isOwner) throw ForbiddenException(Message.Errors.notOwner("review"))
        reviewRatingRepo.deleteReviewRating(reviewId)
    }.also { cache.invalidatePattern(CacheKeys.PRODUCTS_PATTERN) }
}
