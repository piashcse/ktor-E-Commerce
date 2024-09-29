package com.piashcse.repository

import com.piashcse.entities.ReviewRating
import com.piashcse.models.AddReviewRating

interface ReviewRatingRepo {
    suspend fun getReviewRating(productId: String, limit: Int, offset: Long): List<ReviewRating>
    suspend fun addReviewRating(userId: String, reviewRating: AddReviewRating): ReviewRating
    suspend fun updateReviewRating(reviewId: String, review: String, rating: Int): ReviewRating
    suspend fun deleteReviewRating(reviewId: String): String
}