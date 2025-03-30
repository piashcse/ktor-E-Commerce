package com.piashcse.repository

import com.piashcse.entities.ReviewRating
import com.piashcse.models.ReviewRatingRequest

interface ReviewRatingRepo {
    suspend fun getReviewRating(productId: String, limit: Int): List<ReviewRating>
    suspend fun addReviewRating(userId: String, reviewRating: ReviewRatingRequest): ReviewRating
    suspend fun updateReviewRating(reviewId: String, review: String, rating: Int): ReviewRating
    suspend fun deleteReviewRating(reviewId: String): String
}