package com.piashcse.feature.review_rating

import com.piashcse.constants.Message
import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.model.response.ReviewRatingResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.suspendRetryQuery
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException

class ReviewRatingService(private val reviewRatingRepo: ReviewRatingRepository) {
    suspend fun getReviewRating(
        productId: String,
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<ReviewRatingResponse> = reviewRatingRepo.getReviewRating(productId, limit, offset)

    /**
     * Adds a review, enforcing the rating range rule. Runs in a retryable transaction.
     */
    suspend fun addReviewRating(
        userId: String,
        reviewRating: ReviewRatingRequest,
    ): ReviewRatingResponse = suspendRetryQuery {
        if (reviewRating.rating < 1 || reviewRating.rating > 5)
            throw ValidationException(Message.Validation.RATING_OUT_OF_RANGE)
        reviewRatingRepo.addReviewRating(userId, reviewRating)
    }

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
    }

    /**
     * Deletes a review after verifying ownership. Runs in a retryable transaction.
     */
    suspend fun deleteReviewRating(userId: String, reviewId: String): String = suspendRetryQuery {
        val access = reviewRatingRepo.getReviewAccess(userId, reviewId)
        if (!access.isOwner) throw ForbiddenException(Message.Errors.notOwner("review"))
        reviewRatingRepo.deleteReviewRating(reviewId)
    }
}
