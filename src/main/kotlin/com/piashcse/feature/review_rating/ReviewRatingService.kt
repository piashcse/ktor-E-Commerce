package com.piashcse.feature.review_rating

import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.model.response.ReviewRatingResponse
import com.piashcse.utils.common.PaginatedResponse

class ReviewRatingService(private val reviewRatingRepo: ReviewRatingRepository) {
    suspend fun getReviewRating(
        productId: String,
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<ReviewRatingResponse> = reviewRatingRepo.getReviewRating(productId, limit, offset)

    suspend fun addReviewRating(
        userId: String,
        reviewRating: ReviewRatingRequest,
    ): ReviewRatingResponse = reviewRatingRepo.addReviewRating(userId, reviewRating)

    suspend fun updateReviewRating(
        userId: String,
        reviewId: String,
        review: String,
        rating: Int,
    ): ReviewRatingResponse = reviewRatingRepo.updateReviewRating(userId, reviewId, review, rating)

    suspend fun deleteReviewRating(userId: String, reviewId: String): String =
        reviewRatingRepo.deleteReviewRating(userId, reviewId)
}
