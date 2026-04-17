package com.piashcse.feature.review_rating

import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.model.response.ReviewRating
import com.piashcse.utils.PaginatedResponse

interface ReviewRatingRepository {
    suspend fun getReviewRating(productId: String, limit: Int, offset: Int = 0): PaginatedResponse<ReviewRating>
    suspend fun addReviewRating(userId: String, reviewRating: ReviewRatingRequest): ReviewRating
    suspend fun updateReviewRating(reviewId: String, review: String, rating: Int): ReviewRating
    suspend fun deleteReviewRating(reviewId: String): String
}