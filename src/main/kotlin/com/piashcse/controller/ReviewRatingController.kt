package com.piashcse.controller

import com.piashcse.entities.ReviewRating
import com.piashcse.entities.ReviewRatingEntity
import com.piashcse.entities.ReviewRatingTable
import com.piashcse.models.AddReviewRating
import com.piashcse.repository.ReviewRatingRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

class ReviewRatingController : ReviewRatingRepo {
    override suspend fun getReviewRating(
        productId: String, limit: Int, offset: Long
    ): List<ReviewRating> = query {
        val isProductIdExist =
            ReviewRatingEntity.find { ReviewRatingTable.productId eq productId }
                .limit(limit, offset)
        isProductIdExist.map {
            it.response()
        }
    }

    override suspend fun addReviewRating(userId: String, reviewRating: AddReviewRating): ReviewRating = query {
        val isReviewRatingExist =
            ReviewRatingEntity.find { ReviewRatingTable.id eq userId and (ReviewRatingTable.productId eq reviewRating.productId) }
                .singleOrNull()
        isReviewRatingExist?.let {
            throw it.productId.value.alreadyExistException()
        } ?: ReviewRatingEntity.new {
            this.userId = EntityID(userId, ReviewRatingTable)
            productId = EntityID(reviewRating.productId, ReviewRatingTable)
            reviewText = reviewRating.reviewText
            rating = reviewRating.rating
        }.response()
    }

    override suspend fun updateReviewRating(
        reviewId: String,
        review: String,
        rating: Int
    ): ReviewRating = query {
        val isReviewRatingExist =
            ReviewRatingEntity.find { ReviewRatingTable.id eq reviewId }
                .singleOrNull()

        isReviewRatingExist?.let {
            it.reviewText = review
            it.rating = rating
            it.response()
        } ?: throw review.notFoundException()
    }

    override suspend fun deleteReviewRating(reviewId: String): String = query {
        val isReviewRatingExist =
            ReviewRatingEntity.find {ReviewRatingTable.id eq reviewId }
                .singleOrNull()
        isReviewRatingExist?.let {
            it.delete()
            reviewId
        } ?: throw reviewId.notFoundException()
    }
}