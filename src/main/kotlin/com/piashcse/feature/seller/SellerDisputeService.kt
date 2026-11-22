package com.piashcse.feature.seller

import com.piashcse.constants.DisputeStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.DisputeMessageDAO
import com.piashcse.database.entities.DisputeMessageTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerDisputeDAO
import com.piashcse.database.entities.SellerDisputeTable
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.DisputeCreatedEvent
import com.piashcse.event.EventBus
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.findSellerByUserId
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Seller dispute creation, messaging and moderation. */
class SellerDisputeService {
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
