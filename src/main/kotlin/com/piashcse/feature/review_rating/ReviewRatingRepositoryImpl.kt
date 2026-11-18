package com.piashcse.feature.review_rating

import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.ReviewStatus
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.OrderItemTable
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.ReviewHelpfulVoteDAO
import com.piashcse.database.entities.ReviewHelpfulVoteTable
import com.piashcse.database.entities.ReviewImageDAO
import com.piashcse.database.entities.ReviewImageTable
import com.piashcse.database.entities.ReviewRatingDAO
import com.piashcse.database.entities.ReviewRatingTable
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.EventBus
import com.piashcse.event.ProductReviewCreatedEvent
import com.piashcse.mapper.toReviewRatingResponse
import com.piashcse.model.request.ReviewRatingRequest
import com.piashcse.model.response.RatingBucket
import com.piashcse.model.response.ReviewRatingResponse
import com.piashcse.model.response.ReviewSummaryResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal

class ReviewRatingRepositoryImpl : ReviewRatingRepository {
    private fun recalculateProductRating(productId: String) {
        val product = ProductDAO.findById(productId) ?: return
        val counts =
            (1..5).associateWith { rating ->
                ReviewRatingDAO.find {
                    (ReviewRatingTable.productId eq productId) and
                        (ReviewRatingTable.status eq ReviewStatus.ACTIVE) and
                        (ReviewRatingTable.rating eq rating)
                }.count().toInt()
            }
        val totalReviews = counts.values.sum()
        if (totalReviews == 0) {
            product.rating = BigDecimal.ZERO
            product.totalReviews = 0
        } else {
            val weighted = counts.entries.sumOf { (rating, count) -> rating * count }
            product.rating =
                BigDecimal(weighted)
                    .divide(BigDecimal(totalReviews), 2, java.math.RoundingMode.HALF_UP)
            product.totalReviews = totalReviews
        }
    }

    override suspend fun getReviewSummary(productId: String): ReviewSummaryResponse =
        query {
            val counts =
                (1..5).associateWith { rating ->
                    ReviewRatingDAO.find {
                        (ReviewRatingTable.productId eq productId) and
                            (ReviewRatingTable.status eq ReviewStatus.ACTIVE) and
                            (ReviewRatingTable.rating eq rating)
                    }.count().toInt()
                }
            val total = counts.values.sum()
            val weighted = counts.entries.sumOf { (rating, count) -> rating * count }
            val average =
                if (total == 0) {
                    0.0
                } else {
                    BigDecimal(weighted)
                        .divide(BigDecimal(total), 2, java.math.RoundingMode.HALF_UP)
                        .toDouble()
                }
            ReviewSummaryResponse(
                productId = productId,
                total = total,
                average = average,
                buckets = (5 downTo 1).map { RatingBucket(it, counts[it] ?: 0) },
            )
        }

    private fun imagesFor(reviewIds: List<EntityID<String>>): Map<String, List<String>> {
        if (reviewIds.isEmpty()) return emptyMap()
        return ReviewImageDAO.find { ReviewImageTable.reviewId inList reviewIds }
            .orderBy(ReviewImageTable.sortOrder to SortOrder.ASC)
            .groupBy { it.reviewId.value }
            .mapValues { (_, rows) -> rows.map { it.imageUrl } }
    }

    private fun ReviewRatingDAO.toResponseWithImages(includeModeration: Boolean = false): ReviewRatingResponse =
        toReviewRatingResponse(imagesFor(listOf(id))[id.value].orEmpty(), includeModeration)

    override suspend fun getReviewRating(
        productId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ReviewRatingResponse> =
        query {
            val (totalCount, rows) =
                ReviewRatingTable.selectAll()
                    .andWhere { ReviewRatingTable.productId eq productId }
                    .andWhere { ReviewRatingTable.status eq ReviewStatus.ACTIVE }
                    .toPaginatedList(limit, offset) { ReviewRatingDAO.wrapRow(it) }
            val images = imagesFor(rows.map { it.id })
            PaginatedResponse(
                rows.map { it.toReviewRatingResponse(images[it.id.value].orEmpty()) },
                com.piashcse.utils.common.PaginationMetadata(totalCount, limit, offset),
            )
        }

    override suspend fun getReviews(
        status: ReviewStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ReviewRatingResponse> =
        query {
            val query = ReviewRatingTable.selectAll()
            status?.let { query.andWhere { ReviewRatingTable.status eq it } }
            val (totalCount, rows) =
                query.orderBy(ReviewRatingTable.createdAt to SortOrder.DESC)
                    .toPaginatedList(limit, offset) { ReviewRatingDAO.wrapRow(it) }
            val images = imagesFor(rows.map { it.id })
            PaginatedResponse(
                rows.map { it.toReviewRatingResponse(images[it.id.value].orEmpty(), includeModeration = true) },
                com.piashcse.utils.common.PaginationMetadata(totalCount, limit, offset),
            )
        }

    override suspend fun addReviewRating(
        userId: String,
        reviewRating: ReviewRatingRequest,
    ): ReviewRatingResponse {
        val response =
            query {
                if (reviewRating.rating < 1 || reviewRating.rating > 5) {
                    throw ValidationException(Message.Validation.RATING_OUT_OF_RANGE)
                }
                val product =
                    ProductDAO.findById(reviewRating.productId)
                        ?: reviewRating.productId.throwNotFound("Product")
                if (product.deletedAt != null || product.status != com.piashcse.constants.ProductStatus.ACTIVE) {
                    throw ValidationException("Cannot review an inactive product")
                }
                ReviewRatingDAO.find { ReviewRatingTable.userId eq userId and (ReviewRatingTable.productId eq reviewRating.productId) }
                    .singleOrNull()?.let {
                        throw it.productId.value.throwConflict("Review")
                    } ?: run {
                    val paidDeliveredOrderIds =
                        OrderDAO.find {
                            (OrderTable.userId eq userId) and
                                (OrderTable.paymentStatus eq PaymentStatus.COMPLETED) and
                                (OrderTable.status inList listOf(OrderStatus.DELIVERED, OrderStatus.RECEIVED))
                        }.map { it.id }
                    val verified =
                        paidDeliveredOrderIds.isNotEmpty() &&
                            OrderItemDAO.find {
                                (OrderItemTable.productId eq reviewRating.productId.entityID(ProductTable)) and
                                    (OrderItemTable.orderId inList paidDeliveredOrderIds)
                            }.firstOrNull() != null
                    val review =
                        ReviewRatingDAO.new {
                            this.userId = userId.entityID(UserTable)
                            productId = reviewRating.productId.entityID(ProductTable)
                            reviewText = reviewRating.reviewText
                            rating = reviewRating.rating
                            isVerifiedPurchase = verified
                            status = ReviewStatus.ACTIVE
                        }
                    reviewRating.images.forEachIndexed { index, url ->
                        ReviewImageDAO.new {
                            this.reviewId = review.id
                            imageUrl = url
                            sortOrder = index
                        }
                    }
                    recalculateProductRating(reviewRating.productId)
                    review.toResponseWithImages()
                }
            }
        EventBus.publish(
            ProductReviewCreatedEvent(
                response.id,
                response.productId,
                userId,
                response.rating,
                response.reviewText,
            ),
        )
        return response
    }

    override suspend fun updateReviewRating(
        userId: String,
        reviewId: String,
        review: String,
        rating: Int,
    ): ReviewRatingResponse =
        query {
            if (rating < 1 || rating > 5) {
                throw ValidationException(Message.Validation.RATING_OUT_OF_RANGE)
            }
            ReviewRatingDAO.find { ReviewRatingTable.id eq reviewId }
                .singleOrNull()?.let {
                    it.verifyOwnership(userId, "review") { r -> r.userId.value }
                    it.reviewText = review
                    it.rating = rating
                    recalculateProductRating(it.productId.value)
                    it.toResponseWithImages()
                } ?: reviewId.throwNotFound("Review")
        }

    override suspend fun markHelpful(
        userId: String,
        reviewId: String,
        helpful: Boolean,
    ): ReviewRatingResponse =
        query {
            val r = ReviewRatingDAO.findById(reviewId) ?: reviewId.throwNotFound("Review")
            val existingVote =
                ReviewHelpfulVoteDAO.find {
                    (ReviewHelpfulVoteTable.reviewId eq r.id) and (ReviewHelpfulVoteTable.userId eq userId.entityID(UserTable))
                }.firstOrNull()

            if (existingVote == null) {
                ReviewHelpfulVoteDAO.new {
                    this.reviewId = r.id
                    this.userId = userId.entityID(UserTable)
                    this.helpful = helpful
                }
                if (helpful) r.helpfulCount++ else r.notHelpfulCount++
            } else if (existingVote.helpful != helpful) {
                if (existingVote.helpful) {
                    r.helpfulCount = (r.helpfulCount - 1).coerceAtLeast(0)
                    r.notHelpfulCount++
                } else {
                    r.notHelpfulCount = (r.notHelpfulCount - 1).coerceAtLeast(0)
                    r.helpfulCount++
                }
                existingVote.helpful = helpful
            }
            r.toResponseWithImages()
        }

    override suspend fun updateReviewImages(
        userId: String,
        reviewId: String,
        images: List<String>,
    ): ReviewRatingResponse =
        query {
            val review = ReviewRatingDAO.findById(reviewId) ?: reviewId.throwNotFound("Review")
            review.verifyOwnership(userId, "review") { r -> r.userId.value }
            if (images.size > ReviewRatingRequest.MAX_IMAGES) {
                throw ValidationException("At most ${ReviewRatingRequest.MAX_IMAGES} images are allowed")
            }
            if (images.any { it.isBlank() }) throw ValidationException("Image URLs must not be blank")
            ReviewImageDAO.find { ReviewImageTable.reviewId eq review.id }.forEach { it.delete() }
            images.forEachIndexed { index, url ->
                ReviewImageDAO.new {
                    this.reviewId = review.id
                    imageUrl = url
                    sortOrder = index
                }
            }
            review.toResponseWithImages()
        }

    override suspend fun moderateReview(
        reviewId: String,
        status: ReviewStatus,
        reason: String?,
    ): ReviewRatingResponse =
        query {
            val review = ReviewRatingDAO.findById(reviewId) ?: reviewId.throwNotFound("Review")
            review.status = status
            review.moderationReason = reason?.takeIf { it.isNotBlank() }
            recalculateProductRating(review.productId.value)
            review.toResponseWithImages(includeModeration = true)
        }

    override suspend fun replyAsSeller(
        userId: String,
        reviewId: String,
        reply: String,
    ): ReviewRatingResponse =
        query {
            val review = ReviewRatingDAO.findById(reviewId) ?: reviewId.throwNotFound("Review")
            val product = ProductDAO.findById(review.productId.value) ?: throw ValidationException(Message.Products.NOT_FOUND)
            val shopId = product.shopId?.value ?: throw ForbiddenException("Product has no shop")
            val seller = requireSellerByUserId(userId)
            if (!sellerOwnsShop(seller, shopId)) throw ForbiddenException(Message.Products.NOT_SHOP_OWNER)
            review.sellerReply = reply.trim()
            review.sellerRepliedAt = currentUtc()
            review.toResponseWithImages()
        }

    override suspend fun deleteReviewRating(
        userId: String,
        reviewId: String,
    ): String =
        query {
            ReviewRatingDAO.find { ReviewRatingTable.id eq reviewId }
                .singleOrNull()?.let {
                    it.verifyOwnership(userId, "review") { r -> r.userId.value }
                    val productId = it.productId.value
                    ReviewHelpfulVoteDAO.find { ReviewHelpfulVoteTable.reviewId eq it.id }.forEach { vote -> vote.delete() }
                    ReviewImageDAO.find { ReviewImageTable.reviewId eq it.id }.forEach { image -> image.delete() }
                    it.delete()
                    recalculateProductRating(productId)
                    reviewId
                } ?: reviewId.throwNotFound("Review")
        }
}
