package com.piashcse.feature.review_rating

import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.model.response.ReviewRating

interface ReviewRatingRepository {
    suspend fun getReviewRating(productId: String, limit: Int): List<ReviewRating>
    suspend fun addReviewRating(userId: String, reviewRating: ReviewRatingRequest): ReviewRating
    suspend fun updateReviewRating(reviewId: String, review: String, rating: Int): ReviewRating
    suspend fun deleteReviewRating(reviewId: String): String
}