package com.piashcse.feature.rma

import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RefundStatus
import com.piashcse.constants.RmaResolution
import com.piashcse.constants.RmaStatus
import com.piashcse.constants.RmaType
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

@Serializable
data class RmaCreateRequest(
    val orderId: String,
    val orderItemId: String,
    val quantity: Int = 1,
    val type: RmaType = RmaType.RETURN,
    val reason: String,
) {
    init {
        if (quantity <= 0) throw ValidationException("Quantity must be positive")
        if (reason.isBlank()) throw ValidationException("Reason is required")
    }
}

@Serializable
data class RmaShipRequest(val trackingNumber: String)

@Serializable
data class RmaStatusUpdateRequest(
    val status: RmaStatus,
    val resolution: RmaResolution? = null,
    val replacementOrderId: String? = null,
    val note: String? = null,
)

@Serializable
data class RmaResponse(
    val id: String,
    val orderId: String,
    val orderItemId: String,
    val userId: String,
    val shopId: String?,
    val quantity: Int,
    val type: RmaType,
    val status: RmaStatus,
    val reason: String,
    val resolution: RmaResolution?,
    val trackingNumber: String?,
    val replacementOrderId: String?,
    val refundRequestId: String?,
    val createdAt: String,
    val resolvedAt: String?,
)

class RmaService {
    private fun activeStatuses() = listOf(RmaStatus.REQUESTED, RmaStatus.APPROVED, RmaStatus.IN_TRANSIT, RmaStatus.RECEIVED)

    suspend fun create(
        userId: String,
        request: RmaCreateRequest,
    ): RmaResponse =
        query {
            val order = OrderDAO.findById(request.orderId) ?: throw ValidationException(Message.Orders.NOT_FOUND)
            if (order.userId.value != userId) throw ForbiddenException(Message.Orders.UNAUTHORIZED)
            if (order.status !in listOf(OrderStatus.DELIVERED, OrderStatus.RECEIVED)) {
                throw ValidationException("Returns/exchanges can only be requested after delivery")
            }
            if (order.paymentStatus != PaymentStatus.COMPLETED) {
                throw ValidationException("Returns/exchanges require a paid order")
            }
            val item =
                OrderItemDAO.find {
                    (OrderItemTable.id eq request.orderItemId.entityID(OrderItemTable)) and
                        (OrderItemTable.orderId eq order.id)
                }.forUpdate().firstOrNull() ?: throw ValidationException(Message.Refunds.ITEM_NOT_FOUND)
            if (request.quantity > item.quantity) {
                throw ValidationException("RMA quantity cannot exceed the ordered quantity (${item.quantity})")
            }
            val existing =
                RmaDAO.find {
                    (RmaTable.orderItemId eq item.id) and (RmaTable.status inList activeStatuses())
                }.firstOrNull()
            if (existing != null) throw ValidationException("An active return/exchange already exists for this item")

            RmaDAO.new {
                this.orderId = order.id
                this.orderItemId = item.id
                this.userId = order.userId
                this.shopId = order.shopId
                this.quantity = request.quantity
                this.type = request.type
                this.status = RmaStatus.REQUESTED
                this.reason = request.reason
            }.toResponse()
        }

    suspend fun getUserRmas(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RmaResponse> =
        query {
            RmaTable.selectAll().andWhere { RmaTable.userId eq userId }
                .orderBy(RmaTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { RmaDAO.wrapRow(it).toResponse() }
        }

    suspend fun getSellerRmas(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RmaResponse> =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId?.value ?: throw ValidationException(Message.Orders.NO_SHOP_ASSOCIATED)
            RmaTable.selectAll().andWhere { RmaTable.shopId eq shopId }
                .orderBy(RmaTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { RmaDAO.wrapRow(it).toResponse() }
        }

    suspend fun getAdminRmas(
        limit: Int,
        offset: Int,
        status: String?,
    ): PaginatedResponse<RmaResponse> =
        query {
            val query = RmaTable.selectAll()
            status?.let {
                val parsed =
                    runCatching { RmaStatus.valueOf(it.uppercase()) }
                        .getOrElse { throw ValidationException("Invalid RMA status: $it") }
                query.andWhere { RmaTable.status eq parsed }
            }
            query.orderBy(RmaTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { RmaDAO.wrapRow(it).toResponse() }
        }

    suspend fun ship(
        rmaId: String,
        userId: String,
        trackingNumber: String,
    ): RmaResponse =
        query {
            val rma = RmaDAO.findById(rmaId) ?: rmaId.throwNotFound("RMA")
            if (rma.userId.value != userId) throw ForbiddenException(Message.Orders.UNAUTHORIZED)
            if (rma.status != RmaStatus.APPROVED) {
                throw ValidationException("RMA must be approved before shipping")
            }
            trackingNumber.requireNotBlank("Tracking number")
            rma.trackingNumber = trackingNumber
            rma.status = RmaStatus.IN_TRANSIT
            rma.toResponse()
        }

    suspend fun cancel(
        rmaId: String,
        userId: String,
    ): RmaResponse =
        query {
            val rma = RmaDAO.findById(rmaId) ?: rmaId.throwNotFound("RMA")
            if (rma.userId.value != userId) throw ForbiddenException(Message.Orders.UNAUTHORIZED)
            if (rma.status !in listOf(RmaStatus.REQUESTED, RmaStatus.APPROVED)) {
                throw ValidationException("RMA cannot be cancelled from status ${rma.status}")
            }
            rma.status = RmaStatus.CANCELLED
            rma.resolvedAt = currentUtc()
            rma.toResponse()
        }

    suspend fun updateStatus(
        rmaId: String,
        request: RmaStatusUpdateRequest,
        actorUserId: String,
        actorType: UserType,
    ): RmaResponse =
        query {
            val rma = RmaDAO.findById(rmaId) ?: rmaId.throwNotFound("RMA")
            val isAdmin = actorType in listOf(UserType.ADMIN, UserType.SUPER_ADMIN)
            if (!isAdmin) {
                val seller = requireSellerByUserId(actorUserId)
                val shopId = rma.shopId?.value ?: throw ForbiddenException(Message.Errors.FORBIDDEN)
                if (!sellerOwnsShop(seller, shopId)) throw ForbiddenException(Message.Errors.FORBIDDEN)
            }
            if (request.status == RmaStatus.IN_TRANSIT || request.status == RmaStatus.CANCELLED) {
                throw ValidationException("Use the dedicated ship/cancel endpoints for this transition")
            }
            if (!RmaStatus.canTransitionTo(rma.status, request.status)) {
                throw ValidationException("Cannot change RMA status from ${rma.status} to ${request.status}")
            }

            when (request.status) {
                RmaStatus.APPROVED, RmaStatus.REJECTED -> {
                    rma.status = request.status
                    if (request.status == RmaStatus.REJECTED) rma.resolvedAt = currentUtc()
                }
                RmaStatus.RECEIVED -> {
                    rma.status = RmaStatus.RECEIVED
                }
                RmaStatus.COMPLETED -> {
                    val resolution =
                        request.resolution
                            ?: throw ValidationException("Resolution is required when completing an RMA")
                    rma.resolution = resolution
                    if (resolution == RmaResolution.REFUND) {
                        val item =
                            OrderItemDAO.findById(rma.orderItemId.value)
                                ?: throw ValidationException(Message.Refunds.ITEM_NOT_FOUND)
                        val order =
                            OrderDAO.findById(rma.orderId.value)
                                ?: throw ValidationException(Message.Orders.NOT_FOUND)
                        val existingRefund =
                            RefundRequestDAO.find {
                                (RefundRequestTable.orderItemId eq item.id) and
                                    (
                                        RefundRequestTable.status inList
                                            listOf(
                                                RefundStatus.PENDING,
                                                RefundStatus.APPROVED,
                                                RefundStatus.SHIPPED,
                                                RefundStatus.REFUNDED,
                                            )
                                    )
                            }.firstOrNull()
                        val refund =
                            existingRefund ?: RefundRequestDAO.new {
                                this.orderItemId = item.id
                                this.userId = rma.userId
                                this.orderId = rma.orderId
                                this.reason = rma.reason
                                this.currency = order.currency
                                this.quantity = rma.quantity
                                this.status = RefundStatus.PENDING
                            }
                        rma.refundRequestId = refund.id
                    } else {
                        val replacementId =
                            request.replacementOrderId
                                ?: throw ValidationException("replacementOrderId is required for a replacement")
                        val replacement =
                            OrderDAO.findById(replacementId)
                                ?: throw ValidationException("Replacement order not found")
                        if (replacement.userId.value != rma.userId.value) {
                            throw ValidationException("Replacement order must belong to the same customer")
                        }
                        if (replacement.shopId?.value != rma.shopId?.value) {
                            throw ValidationException("Replacement order must belong to the same shop")
                        }
                        val item =
                            OrderItemDAO.findById(rma.orderItemId.value)
                                ?: throw ValidationException(Message.Refunds.ITEM_NOT_FOUND)
                        ProductDAO.findById(item.productId.value)?.let { product ->
                            product.restoreStock(rma.quantity)
                            product.removeSales(rma.quantity)
                        }
                        rma.replacementOrderId = replacement.id
                    }
                    rma.status = RmaStatus.COMPLETED
                    rma.resolvedAt = currentUtc()
                }
                else -> throw ValidationException("Unsupported RMA transition")
            }
            rma.toResponse()
        }

    private fun RmaDAO.toResponse() =
        RmaResponse(
            id = id.value,
            orderId = orderId.value,
            orderItemId = orderItemId.value,
            userId = userId.value,
            shopId = shopId?.value,
            quantity = quantity,
            type = type,
            status = status,
            reason = reason,
            resolution = resolution,
            trackingNumber = trackingNumber,
            replacementOrderId = replacementOrderId?.value,
            refundRequestId = refundRequestId?.value,
            createdAt = createdAt.toUtcString(),
            resolvedAt = resolvedAt?.toUtcString(),
        )
}
