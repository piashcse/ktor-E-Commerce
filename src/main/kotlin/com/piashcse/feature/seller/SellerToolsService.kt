package com.piashcse.feature.seller

import com.piashcse.constants.DisputeStatus
import com.piashcse.constants.DisputeType
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.ReviewStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.DisputeMessageDAO
import com.piashcse.database.entities.DisputeMessageTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerDisputeDAO
import com.piashcse.database.entities.SellerDisputeTable
import com.piashcse.database.entities.SellerReviewDAO
import com.piashcse.database.entities.SellerReviewTable
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.DisputeCreatedEvent
import com.piashcse.event.EventBus
import com.piashcse.event.ReviewReplyEvent
import com.piashcse.event.SellerReviewCreatedEvent
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.findSellerByUserId
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.FieldValidationException
import com.piashcse.utils.validator.FieldViolation
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.RoundingMode

@Serializable
data class SellerReviewRequest(
    val orderId: String,
    val rating: Int,
    val comment: String? = null,
) {
    init {
        if (rating !in 1..5) throw FieldValidationException(listOf(FieldViolation("rating", "Rating must be 1..5")))
    }
}

@Serializable
data class SellerReviewStatusRequest(
    val status: ReviewStatus,
    val reason: String? = null,
)

@Serializable
data class SellerReviewResponse(
    val id: String,
    val sellerId: String,
    val shopId: String?,
    val userId: String? = null,
    val orderId: String? = null,
    val rating: Int,
    val comment: String?,
    val status: ReviewStatus,
    val sellerReply: String? = null,
    val sellerRepliedAt: String? = null,
    val moderationReason: String? = null,
    val createdAt: String?,
)

@Serializable
data class DisputeRequest(
    val orderId: String? = null,
    val sellerId: String? = null,
    val type: DisputeType,
    val subject: String,
    val description: String,
) {
    init {
        if (subject.isBlank()) throw FieldValidationException(listOf(FieldViolation("subject", "Subject is required")))
        if (description.isBlank()) {
            throw FieldValidationException(listOf(FieldViolation("description", "Description is required")))
        }
    }
}

@Serializable
data class DisputeUpdateRequest(
    val status: DisputeStatus,
    val resolution: String? = null,
)

@Serializable
data class DisputeMessageRequest(
    val body: String,
    val isInternal: Boolean = false,
)

@Serializable
data class DisputeMessageResponse(
    val id: String,
    val authorId: String,
    val body: String,
    val isInternal: Boolean,
    val createdAt: String?,
)

@Serializable
data class DisputeDetailResponse(
    val dispute: DisputeResponse,
    val messages: List<DisputeMessageResponse>,
)

@Serializable
data class DisputeResponse(
    val id: String,
    val orderId: String?,
    val sellerId: String?,
    val userId: String,
    val type: DisputeType,
    val subject: String,
    val description: String,
    val status: DisputeStatus,
    val resolution: String?,
    val resolvedBy: String?,
    val resolvedAt: String?,
    val createdAt: String?,
)

class SellerToolsService {
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
            .toPaginatedResponse(limit, offset) { SellerReviewDAO.wrapRow(it).toResponse() }

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
    ): DisputeResponse {
        val response =
            query {
                val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")
                var sellerId: String? = request.sellerId?.takeIf { it.isNotBlank() }
                if (request.orderId != null) {
                    val order = OrderDAO.findById(request.orderId) ?: request.orderId.throwNotFound("Order")
                    val seller =
                        order.shopId?.value?.let { shopId ->
                            SellerDAO.find { SellerTable.shopId eq shopId.entityID(ShopTable) }.firstOrNull()
                        }
                    val isOwner = order.userId.value == userId
                    val isSeller = seller?.userId?.value == userId
                    if (!isOwner && !isSeller && user.userType !in listOf(UserType.ADMIN, UserType.SUPER_ADMIN)) {
                        throw ForbiddenException("You do not have access to this order")
                    }
                    sellerId = seller?.id?.value
                } else if (sellerId == null) {
                    sellerId = runCatching { requireSellerByUserId(userId).id.value }.getOrNull()
                }
                SellerDisputeDAO.new {
                    this.orderId = request.orderId?.entityID(com.piashcse.database.entities.OrderTable)
                    this.sellerId = sellerId?.entityID(SellerTable)
                    this.userId = userId.entityID(com.piashcse.database.entities.UserTable)
                    this.type = request.type
                    this.subject = request.subject
                    this.description = request.description
                    this.status = DisputeStatus.OPEN
                }.toResponse()
            }
        EventBus.publish(DisputeCreatedEvent(response.id, response.sellerId, userId, response.subject))
        return response
    }

    suspend fun myDisputes(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<DisputeResponse> =
        query {
            val sellerIds =
                SellerDAO.find { SellerTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable) }
                    .map { it.id }
            SellerDisputeTable.selectAll()
                .where {
                    (SellerDisputeTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable)) or
                        (SellerDisputeTable.sellerId inList sellerIds)
                }.orderBy(SellerDisputeTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { SellerDisputeDAO.wrapRow(it).toResponse() }
        }

    suspend fun adminDisputes(
        status: DisputeStatus?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<DisputeResponse> =
        query {
            SellerDisputeTable.selectAll()
                .also { query ->
                    status?.let { query.andWhere { SellerDisputeTable.status eq it } }
                    query.orderBy(SellerDisputeTable.createdAt to SortOrder.DESC)
                }.toPaginatedResponse(limit, offset) { SellerDisputeDAO.wrapRow(it).toResponse() }
        }

    suspend fun updateDispute(
        disputeId: String,
        actorUserId: String,
        request: DisputeUpdateRequest,
    ): DisputeResponse =
        query {
            val dispute = SellerDisputeDAO.findById(disputeId) ?: disputeId.throwNotFound("Dispute")
            if (!canTransition(dispute.status, request.status)) {
                throw ValidationException("Invalid dispute transition: ${dispute.status} -> ${request.status}")
            }
            dispute.status = request.status
            request.resolution?.let { dispute.resolution = it }
            if (request.status in listOf(DisputeStatus.RESOLVED, DisputeStatus.REJECTED)) {
                dispute.resolvedBy = actorUserId.entityID(com.piashcse.database.entities.UserTable)
                dispute.resolvedAt = currentUtc()
            }
            dispute.toResponse()
        }

    private fun canTransition(
        from: DisputeStatus,
        to: DisputeStatus,
    ): Boolean =
        when (from) {
            DisputeStatus.OPEN -> to in listOf(DisputeStatus.UNDER_REVIEW, DisputeStatus.RESOLVED, DisputeStatus.REJECTED)
            DisputeStatus.UNDER_REVIEW -> to in listOf(DisputeStatus.RESOLVED, DisputeStatus.REJECTED)
            DisputeStatus.RESOLVED, DisputeStatus.REJECTED -> false
        }

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

    private fun SellerDisputeDAO.toResponse() =
        DisputeResponse(
            id = id.value,
            orderId = orderId?.value,
            sellerId = sellerId?.value,
            userId = userId.value,
            type = type,
            subject = subject,
            description = description,
            status = status,
            resolution = resolution,
            resolvedBy = resolvedBy?.value,
            resolvedAt = resolvedAt?.toUtcString(),
            createdAt = createdAt.toUtcString(),
        )

    private fun DisputeMessageDAO.toResponse() =
        DisputeMessageResponse(
            id = id.value,
            authorId = authorId.value,
            body = body,
            isInternal = isInternal,
            createdAt = createdAt.toUtcString(),
        )

    suspend fun disputeDetail(
        userId: String,
        userType: UserType,
        disputeId: String,
    ): DisputeDetailResponse =
        query {
            val dispute = loadDisputeForActor(userId, userType, disputeId)
            val isAdmin = userType in listOf(UserType.ADMIN, UserType.SUPER_ADMIN)
            val messages =
                DisputeMessageDAO.find { DisputeMessageTable.disputeId eq dispute.id }
                    .filter { isAdmin || !it.isInternal }
                    .sortedBy { it.createdAt }
                    .map { it.toResponse() }
            DisputeDetailResponse(dispute.toResponse(), messages)
        }

    suspend fun addDisputeMessage(
        userId: String,
        userType: UserType,
        disputeId: String,
        request: DisputeMessageRequest,
    ): DisputeDetailResponse =
        query {
            val dispute = loadDisputeForActor(userId, userType, disputeId)
            val isAdmin = userType in listOf(UserType.ADMIN, UserType.SUPER_ADMIN)
            if (request.isInternal && !isAdmin) throw ForbiddenException("Only admins can add internal notes")
            if (request.body.isBlank()) throw ValidationException("Message body is required")
            DisputeMessageDAO.new {
                this.disputeId = dispute.id
                authorId = userId.entityID(UserTable)
                body = request.body.trim()
                isInternal = request.isInternal
            }
            val messages =
                DisputeMessageDAO.find { DisputeMessageTable.disputeId eq dispute.id }
                    .filter { isAdmin || !it.isInternal }
                    .sortedBy { it.createdAt }
                    .map { it.toResponse() }
            DisputeDetailResponse(dispute.toResponse(), messages)
        }

    private fun loadDisputeForActor(
        userId: String,
        userType: UserType,
        disputeId: String,
    ): SellerDisputeDAO {
        val dispute = SellerDisputeDAO.findById(disputeId) ?: disputeId.throwNotFound("Dispute")
        val isAdmin = userType in listOf(UserType.ADMIN, UserType.SUPER_ADMIN)
        val seller = findSellerByUserId(userId)
        val isParticipant =
            dispute.userId.value == userId ||
                (dispute.sellerId != null && seller?.id?.value == dispute.sellerId?.value)
        if (!isAdmin && !isParticipant) throw ForbiddenException("You do not have access to this dispute")
        return dispute
    }
}
