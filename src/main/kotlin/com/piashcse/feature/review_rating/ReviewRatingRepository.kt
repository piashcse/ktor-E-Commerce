package com.piashcse.feature.review_rating

import com.piashcse.constants.ReviewStatus
import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.model.response.ReviewRatingResponse
import com.piashcse.utils.common.PaginatedResponse

interface ReviewRatingRepository {
    suspend fun getReviewRating(
        productId: String,
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<ReviewRatingResponse>

    suspend fun addReviewRating(
        userId: String,
        reviewRating: ReviewRatingRequest,
    ): ReviewRatingResponse

    suspend fun updateReviewRating(
        userId: String,
        reviewId: String,
        review: String,
        rating: Int,
    ): ReviewRatingResponse

    suspend fun deleteReviewRating(
        userId: String,
        reviewId: String,
    ): String

    suspend fun markHelpful(
        userId: String,
        reviewId: String,
        helpful: Boolean = true,
    ): ReviewRatingResponse

    suspend fun updateReviewImages(
        userId: String,
        reviewId: String,
        images: List<String>,
    ): ReviewRatingResponse

    suspend fun getReviews(
        status: ReviewStatus?,
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<ReviewRatingResponse>

    suspend fun moderateReview(
        reviewId: String,
        status: ReviewStatus,
    ): ReviewRatingResponse
}
