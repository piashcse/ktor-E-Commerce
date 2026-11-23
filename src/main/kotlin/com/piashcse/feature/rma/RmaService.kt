package com.piashcse.feature.rma

import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RefundStatus
import com.piashcse.constants.RmaResolution
import com.piashcse.constants.RmaStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ConflictException
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList

class RmaService {
    private fun activeStatuses() =
        listOf(RmaStatus.REQUESTED, RmaStatus.APPROVED, RmaStatus.IN_TRANSIT, RmaStatus.RECEIVED, RmaStatus.COMPLETED)

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

            val rma =
                RmaDAO.new {
                    this.orderId = order.id
                    this.orderItemId = item.id
                    this.userId = order.userId
                    this.shopId = order.shopId
                    this.quantity = request.quantity
                    this.type = request.type
                    this.status = RmaStatus.REQUESTED
                    this.reason = request.reason
                }
            RmaEvents.enqueueStatusChange(rma, null, RmaStatus.REQUESTED)
            rma.toRmaResponse()
        }

    suspend fun getUserRmas(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RmaResponse> = RmaQueries.getUserRmas(userId, limit, offset)

    suspend fun getSellerRmas(
        sellerUserId: String,
        status: RmaStatus?,
        search: String?,
        from: java.time.Instant?,
        to: java.time.Instant?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RmaResponse> = RmaQueries.getSellerRmas(sellerUserId, status, search, from, to, limit, offset)

    suspend fun getAdminRmas(
        limit: Int,
        offset: Int,
        status: RmaStatus?,
        search: String?,
        from: java.time.Instant?,
        to: java.time.Instant?,
    ): PaginatedResponse<RmaResponse> = RmaQueries.getAdminRmas(limit, offset, status, search, from, to)

    suspend fun ship(
        rmaId: String,
        userId: String,
        trackingNumber: String,
    ): RmaResponse = RmaLifecycleService.ship(rmaId, userId, trackingNumber)

    suspend fun cancel(
        rmaId: String,
        userId: String,
    ): RmaResponse = RmaLifecycleService.cancel(rmaId, userId)

    suspend fun updateStatus(
        rmaId: String,
        request: RmaStatusUpdateRequest,
        actorUserId: String,
        actorType: UserType,
    ): RmaResponse =
        query {
            val rma =
                RmaDAO.find { RmaTable.id eq rmaId.entityID(RmaTable) }
                    .forUpdate()
                    .firstOrNull()
                    ?: rmaId.throwNotFound("RMA")
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
                throw ConflictException("Cannot change RMA status from ${rma.status} to ${request.status}")
            }
            request.note?.takeIf { it.isNotBlank() }?.let { rma.resolutionNote = it }
            val fromStatus = rma.status

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
                        completeWithRefund(rma, fromStatus)?.let { return@query it }
                    } else {
                        completeWithReplacement(rma, request)
                    }
                    rma.status = RmaStatus.COMPLETED
                    rma.resolvedAt = currentUtc()
                }
                else -> throw ValidationException("Unsupported RMA transition")
            }
            RmaEvents.enqueueStatusChange(rma, fromStatus, rma.status)
            rma.toRmaResponse()
        }

    /** Creates an open refund for the RMA, leaving it at RECEIVED for a second completion. */
    private fun completeWithRefund(
        rma: RmaDAO,
        fromStatus: RmaStatus,
    ): RmaResponse? {
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
        if (existingRefund == null) {
            val alreadyRefundedQuantity =
                RefundRequestDAO.find {
                    (RefundRequestTable.orderItemId eq item.id) and
                        (RefundRequestTable.status eq RefundStatus.REFUNDED)
                }.fold(0) { acc, row -> acc + row.quantity }
            if (alreadyRefundedQuantity + rma.quantity > item.quantity) {
                throw ValidationException(
                    "RMA quantity exceeds the remaining ordered quantity " +
                        "(${item.quantity - alreadyRefundedQuantity})",
                )
            }
            val refund =
                RefundRequestDAO.new {
                    this.orderItemId = item.id
                    this.userId = rma.userId
                    this.orderId = rma.orderId
                    this.reason = rma.reason
                    this.currency = order.currency
                    this.quantity = rma.quantity
                    this.status = RefundStatus.PENDING
                }
            rma.refundRequestId = refund.id
            rma.status = RmaStatus.RECEIVED
            RmaEvents.enqueueStatusChange(rma, fromStatus, rma.status)
            return rma.toRmaResponse()
        }
        if (existingRefund.status != RefundStatus.REFUNDED) {
            throw ValidationException(
                "Refund ${existingRefund.id.value} is still ${existingRefund.status}; settle it before completing the RMA",
            )
        }
        if (existingRefund.quantity < rma.quantity) {
            throw ValidationException(
                "Refund ${existingRefund.id.value} covers fewer units than this RMA",
            )
        }
        rma.refundRequestId = existingRefund.id
        return null
    }

    /** Links a replacement order and returns the original units to stock. */
    private fun completeWithReplacement(
        rma: RmaDAO,
        request: RmaStatusUpdateRequest,
    ) {
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
            if (!product.isDigital) {
                product.restoreStockFor(item.variantId?.value, rma.quantity)
            }
            product.removeSales(rma.quantity)
        }
        rma.replacementOrderId = replacement.id
    }
}
