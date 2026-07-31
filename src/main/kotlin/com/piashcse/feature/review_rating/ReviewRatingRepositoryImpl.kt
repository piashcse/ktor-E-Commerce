package com.piashcse.feature.review_rating

import com.piashcse.constants.Message
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.ReviewRatingDAO
import com.piashcse.database.entities.ReviewRatingTable
import com.piashcse.database.entities.UserTable
import com.piashcse.mapper.toReviewRatingResponse
import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.model.response.ReviewRatingResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import java.math.RoundingMode

class ReviewRatingRepositoryImpl : ReviewRatingRepository {
    private fun recalculateProductRating(productId: String) {
        val reviews = ReviewRatingDAO.find { ReviewRatingTable.productId eq productId }.toList()
        val product = ProductDAO.findById(productId) ?: return
        if (reviews.isEmpty()) {
            product.rating = BigDecimal.ZERO
            product.totalReviews = 0
        } else {
            val total = reviews.map { it.rating.toBigDecimal() }.reduce(BigDecimal::add)
            product.rating = total.divide(reviews.size.toBigDecimal(), 2, RoundingMode.HALF_UP)
            product.totalReviews = reviews.size
        }
    }

    override suspend fun getReviewRating(
        productId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ReviewRatingResponse> =
        query {
            ReviewRatingTable.selectAll().andWhere { ReviewRatingTable.productId eq productId }
                .toPaginatedResponse(limit, offset) {
                    ReviewRatingDAO.wrapRow(it).toReviewRatingResponse()
                }
        }

    override suspend fun addReviewRating(
        userId: String,
        reviewRating: ReviewRatingRequest,
    ): ReviewRatingResponse =
        query {
            if (reviewRating.rating < 1 || reviewRating.rating > 5)
                throw ValidationException(Message.Validation.RATING_OUT_OF_RANGE)
            ReviewRatingDAO.find { ReviewRatingTable.userId eq userId and (ReviewRatingTable.productId eq reviewRating.productId) }
                .singleOrNull()?.let {
                throw it.productId.value.throwConflict("Product")
            } ?: run {
                val review =
                    ReviewRatingDAO.new {
                        this.userId = userId.entityID(UserTable)
                        productId = reviewRating.productId.entityID(ProductTable)
                        reviewText = reviewRating.reviewText
                        rating = reviewRating.rating
                    }
                recalculateProductRating(reviewRating.productId)
                review.toReviewRatingResponse()
            }
        }

    override suspend fun updateReviewRating(
        userId: String,
        reviewId: String,
        review: String,
        rating: Int,
    ): ReviewRatingResponse =
        query {
            if (rating < 1 || rating > 5)
                throw ValidationException(Message.Validation.RATING_OUT_OF_RANGE)
            ReviewRatingDAO.find { ReviewRatingTable.id eq reviewId }
                .singleOrNull()?.let {
                it.verifyOwnership(userId, "review") { r -> r.userId.value }
                it.reviewText = review
                it.rating = rating
                recalculateProductRating(it.productId.value)
                it.toReviewRatingResponse()
            } ?: review.throwNotFound("Review")
        }

    override suspend fun deleteReviewRating(userId: String, reviewId: String): String =
        query {
            ReviewRatingDAO.find { ReviewRatingTable.id eq reviewId }
                .singleOrNull()?.let {
                it.verifyOwnership(userId, "review") { r -> r.userId.value }
                val productId = it.productId.value
                it.delete()
                recalculateProductRating(productId)
                reviewId
            } ?: reviewId.throwNotFound("Review")
        }
}
