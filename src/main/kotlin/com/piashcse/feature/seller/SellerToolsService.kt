package com.piashcse.feature.seller

import com.piashcse.constants.DisputeStatus
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.ReviewStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerReviewDAO
import com.piashcse.database.entities.SellerReviewTable
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.EventBus
import com.piashcse.event.ReviewReplyEvent
import com.piashcse.event.SellerReviewCreatedEvent
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.RoundingMode

class SellerToolsService {
    private val disputes = SellerDisputeService()

    suspend fun createReview(
        userId: String,
        request: SellerReviewRequest,
    ): SellerReviewResponse {
        val response =
            query {
                val order = OrderDAO.findById(request.orderId) ?: request.orderId.throwNotFound("Order")
                if (order.userId.value != userId) throw ForbiddenException("You can only review your own orders")
                if (order.status !in listOf(OrderStatus.DELIVERED, OrderStatus.RECEIVED)) {
                    throw ValidationException("Seller reviews are available after delivery")
                }
                val shopId = order.shopId?.value ?: throw ValidationException("Order has no seller")
                val seller =
                    SellerDAO.find { SellerTable.shopId eq shopId.entityID(ShopTable) }.firstOrNull()
                        ?: throw ValidationException("Seller not found")
                if (SellerReviewDAO.find {
                        (SellerReviewTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable)) and
                            (SellerReviewTable.orderId eq order.id)
                    }.any()
                ) {
                    throw ValidationException("This order has already been reviewed")
                }
                val review =
                    SellerReviewDAO.new {
                        this.sellerId = seller.id
                        this.shopId = seller.shopId
                        this.userId = userId.entityID(com.piashcse.database.entities.UserTable)
                        this.orderId = order.id
                        this.rating = request.rating
                        this.comment = request.comment
                        this.status = ReviewStatus.ACTIVE
                    }
                refreshShopRating(seller.shopId?.value)
                review.toResponse()
            }
        EventBus.publish(
            SellerReviewCreatedEvent(response.id, response.sellerId, userId, response.rating, response.comment),
        )
        return response
    }

    suspend fun reviewsForSeller(
        sellerId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<SellerReviewResponse> =
        query {
            val seller = SellerDAO.findById(sellerId) ?: sellerId.throwNotFound("Seller")
            reviewsForSellerTx(seller.id.value, limit, offset, includePrivate = false)
        }

    suspend fun reviewsForSellerUser(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<SellerReviewResponse> =
        query {
            // DAO lookups require an active Exposed transaction; resolving the seller
            // before opening one resulted in "No transaction in context" (500s).
            val seller = requireSellerByUserId(sellerUserId)
            reviewsForSellerTx(seller.id.value, limit, offset)
        }

    private fun reviewsForSellerTx(
        sellerId: String,
        limit: Int,
        offset: Int,
        includePrivate: Boolean = true,
    ): PaginatedResponse<SellerReviewResponse> =
        SellerReviewTable.selectAll()
            .where {
                (SellerReviewTable.sellerId eq sellerId.entityID(SellerTable)) and
                    (SellerReviewTable.status eq ReviewStatus.ACTIVE)
            }.orderBy(SellerReviewTable.createdAt to SortOrder.DESC)
            .toPaginatedResponse(limit, offset) {
                SellerReviewDAO.wrapRow(it).toResponse(includePrivate = includePrivate)
            }

    /** Public shop reviews, exposed so the storefront can show seller reputation. */
    suspend fun reviewsForShop(
        shopId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<SellerReviewResponse> =
        query {
            SellerReviewTable.selectAll()
                .where {
                    (SellerReviewTable.shopId eq shopId.entityID(ShopTable)) and
                        (SellerReviewTable.status eq ReviewStatus.ACTIVE)
                }.orderBy(SellerReviewTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    SellerReviewDAO.wrapRow(it).toResponse(includePrivate = false)
                }
        }

    /** The signed-in customer's own seller reviews. */
    suspend fun reviewsForUser(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<SellerReviewResponse> =
        query {
            SellerReviewTable.selectAll()
                .where { SellerReviewTable.userId eq userId.entityID(UserTable) }
                .orderBy(SellerReviewTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { SellerReviewDAO.wrapRow(it).toResponse() }
        }

    suspend fun adminReviews(
        status: ReviewStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<SellerReviewResponse> =
        query {
            SellerReviewTable.selectAll()
                .also { query ->
                    status?.let { query.andWhere { SellerReviewTable.status eq it } }
                    query.orderBy(SellerReviewTable.createdAt to SortOrder.DESC)
                }.toPaginatedResponse(limit, offset) {
                    SellerReviewDAO.wrapRow(it).toResponse(includeModeration = true)
                }
        }

    suspend fun updateReviewStatus(
        reviewId: String,
        status: ReviewStatus,
        reason: String? = null,
    ): SellerReviewResponse =
        query {
            val review = SellerReviewDAO.findById(reviewId) ?: reviewId.throwNotFound("Seller review")
            review.status = status
            review.moderationReason = reason?.takeIf { it.isNotBlank() }
            refreshShopRating(review.shopId?.value)
            review.toResponse(includeModeration = true)
        }

    /** Seller reply on a review left for their shop. */
    suspend fun replyToSellerReview(
        sellerUserId: String,
        reviewId: String,
        reply: String,
    ): SellerReviewResponse {
        val response =
            query {
                val seller = requireSellerByUserId(sellerUserId)
                val review = SellerReviewDAO.findById(reviewId) ?: reviewId.throwNotFound("Seller review")
                if (review.sellerId.value != seller.id.value) throw ForbiddenException("Review belongs to another seller")
                review.sellerReply = reply.trim()
                review.sellerRepliedAt = currentUtc()
                review.toResponse()
            }
        response.userId?.let { EventBus.publish(ReviewReplyEvent(response.id, it, response.sellerReply.orEmpty())) }
        return response
    }

    suspend fun createDispute(
        userId: String,
        request: DisputeRequest,
    ): DisputeResponse = disputes.createDispute(userId, request)

    suspend fun myDisputes(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<DisputeResponse> = disputes.myDisputes(userId, limit, offset)

    suspend fun adminDisputes(
        status: DisputeStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<DisputeResponse> = disputes.adminDisputes(status, limit, offset)

    suspend fun updateDispute(
        disputeId: String,
        actorUserId: String,
        request: DisputeUpdateRequest,
    ): DisputeResponse = disputes.updateDispute(disputeId, actorUserId, request)

    private fun refreshShopRating(shopId: String?) {
        if (shopId == null) return
        val shop = ShopDAO.findById(shopId) ?: return
        val active =
            SellerReviewDAO.find {
                (SellerReviewTable.shopId eq shop.id) and (SellerReviewTable.status eq ReviewStatus.ACTIVE)
            }.toList()
        shop.totalReviews = active.size
        shop.rating =
            if (active.isEmpty()) {
                java.math.BigDecimal.ZERO
            } else {
                java.math.BigDecimal(active.sumOf { it.rating })
                    .divide(java.math.BigDecimal(active.size), 2, RoundingMode.HALF_UP)
            }
    }

    private fun SellerReviewDAO.toResponse(
        includeModeration: Boolean = false,
        includePrivate: Boolean = true,
    ) = SellerReviewResponse(
        id = id.value,
        sellerId = sellerId.value,
        shopId = shopId?.value,
        userId = userId.value.takeIf { includePrivate },
        orderId = orderId.value.takeIf { includePrivate },
        rating = rating,
        comment = comment,
        status = status,
        sellerReply = sellerReply,
        sellerRepliedAt = sellerRepliedAt?.toUtcString(),
        moderationReason = moderationReason.takeIf { includeModeration },
        createdAt = createdAt.toUtcString(),
    )

    suspend fun disputeDetail(
        userId: String,
        userType: UserType,
        disputeId: String,
    ): DisputeDetailResponse = disputes.disputeDetail(userId, userType, disputeId)

    suspend fun addDisputeMessage(
        userId: String,
        userType: UserType,
        disputeId: String,
        request: DisputeMessageRequest,
    ): DisputeDetailResponse = disputes.addDisputeMessage(userId, userType, disputeId, request)
}
