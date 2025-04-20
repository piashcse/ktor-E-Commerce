package com.piashcse.modules.review_rating.controller

import com.piashcse.database.entities.ReviewRating
import com.piashcse.database.entities.ReviewRatingDAO
import com.piashcse.database.entities.ReviewRatingTable
import com.piashcse.database.models.ReviewRatingRequest
import com.piashcse.modules.review_rating.repository.ReviewRatingRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

/**
 * Controller for managing product reviews and ratings. Provides methods to get, add, update, and delete review ratings.
 */
class ReviewRatingController : ReviewRatingRepo {

    /**
     * Retrieves a list of reviews and ratings for a product.
     *
     * @param productId The ID of the product whose reviews and ratings are to be retrieved.
     * @param limit The maximum number of reviews to retrieve.
     * @return A list of reviews and ratings for the specified product.
     */
    override suspend fun getReviewRating(
        productId: String, limit: Int
    ): List<ReviewRating> = query {
        val isProductIdExist =
            ReviewRatingDAO.Companion.find { ReviewRatingTable.productId eq productId }
                .limit(limit)
        isProductIdExist.map {
            it.response()
        }
    }

    /**
     * Adds a new review and rating for a product. If the user has already reviewed the product, an exception is thrown.
     *
     * @param userId The ID of the user adding the review and rating.
     * @param reviewRating The review and rating details to be added.
     * @return The added review and rating.
     * @throws alreadyExistException If the user has already reviewed the specified product.
     */
    override suspend fun addReviewRating(userId: String, reviewRating: ReviewRatingRequest): ReviewRating = query {
        val isReviewRatingExist =
            ReviewRatingDAO.Companion.find { ReviewRatingTable.id eq userId and (ReviewRatingTable.productId eq reviewRating.productId) }
                .singleOrNull()
        isReviewRatingExist?.let {
            throw it.productId.value.alreadyExistException()
        } ?: ReviewRatingDAO.Companion.new {
            this.userId = EntityID(userId, ReviewRatingTable)
            productId = EntityID(reviewRating.productId, ReviewRatingTable)
            reviewText = reviewRating.reviewText
            rating = reviewRating.rating
        }.response()
    }

    /**
     * Updates an existing review and rating.
     *
     * @param reviewId The ID of the review to be updated.
     * @param review The updated review text.
     * @param rating The updated rating value.
     * @return The updated review and rating.
     * @throws review.notFoundException() If the review ID does not exist.
     */
    override suspend fun updateReviewRating(
        reviewId: String,
        review: String,
        rating: Int
    ): ReviewRating = query {
        val isReviewRatingExist =
            ReviewRatingDAO.Companion.find { ReviewRatingTable.id eq reviewId }
                .singleOrNull()

        isReviewRatingExist?.let {
            it.reviewText = review
            it.rating = rating
            it.response()
        } ?: throw review.notFoundException()
    }

    /**
     * Deletes a review and rating by its ID.
     *
     * @param reviewId The ID of the review to be deleted.
     * @return The ID of the deleted review.
     * @throws reviewId.notFoundException() If the review ID does not exist.
     */
    override suspend fun deleteReviewRating(reviewId: String): String = query {
        val isReviewRatingExist =
            ReviewRatingDAO.Companion.find { ReviewRatingTable.id eq reviewId }
                .singleOrNull()
        isReviewRatingExist?.let {
            it.delete()
            reviewId
        } ?: throw reviewId.notFoundException()
    }
}