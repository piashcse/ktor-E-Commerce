package com.piashcse.feature.review_rating

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
import com.piashcse.utils.money.Money
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal

class ReviewRatingRepositoryImpl : ReviewRatingRepository {
    private fun recalculateProductRating(productId: String) {
        val reviews = ReviewRatingDAO.find { ReviewRatingTable.productId eq productId }.toList()
        val product = ProductDAO.findById(productId) ?: return
        if (reviews.isEmpty()) {
            product.rating = BigDecimal.ZERO
            product.totalReviews = 0
        } else {
            val total = reviews.map { it.rating.toBigDecimal() }.reduce(BigDecimal::add)
            product.rating = Money.average(total, reviews.size)
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

    override suspend fun getReviewAccess(
        userId: String,
        reviewId: String,
    ): ReviewAccess = query {
        val review = ReviewRatingDAO.findById(reviewId) ?: reviewId.throwNotFound("Review")
        ReviewAccess(
            reviewId = review.id.value,
            productId = review.productId.value,
            isOwner = review.userId.value == userId,
        )
    }

    override suspend fun addReviewRating(
        userId: String,
        reviewRating: ReviewRatingRequest,
    ): ReviewRatingResponse =
        query {
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
        reviewId: String,
        review: String,
        rating: Int,
    ): ReviewRatingResponse =
        query {
            val reviewDao = ReviewRatingDAO.findById(reviewId) ?: reviewId.throwNotFound("Review")
            reviewDao.reviewText = review
            reviewDao.rating = rating
            recalculateProductRating(reviewDao.productId.value)
            reviewDao.toReviewRatingResponse()
        }

    override suspend fun deleteReviewRating(reviewId: String): String =
        query {
            val reviewDao = ReviewRatingDAO.findById(reviewId) ?: reviewId.throwNotFound("Review")
            val productId = reviewDao.productId.value
            reviewDao.delete()
            recalculateProductRating(productId)
            reviewId
        }
}
