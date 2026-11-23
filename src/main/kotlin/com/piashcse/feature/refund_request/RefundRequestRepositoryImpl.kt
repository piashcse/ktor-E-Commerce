package com.piashcse.feature.refund_request

import com.piashcse.constants.Message
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RefundStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.feature.payment.ManualPaymentGateway
import com.piashcse.feature.payment.PaymentGateway
import com.piashcse.mapper.toRefundRequestResponse
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.model.response.RefundRequestResponse
import com.piashcse.service.AppMetrics
import com.piashcse.service.CatalogCache
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ConflictException
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import java.math.BigDecimal

class RefundRequestRepositoryImpl(
    private val gateway: PaymentGateway = ManualPaymentGateway(),
) : RefundRequestRepository {
    private val provider = RefundProviderService(gateway)

    private fun liveStatuses() = listOf(RefundStatus.PENDING, RefundStatus.APPROVED, RefundStatus.SHIPPED)

    override suspend fun createRefundRequest(
        userId: String,
        orderId: String,
        request: RefundRequestRequest,
    ): RefundRequestResponse =
        query {
            val order =
                OrderDAO.findById(orderId)
                    ?: throw ValidationException(Message.Orders.NOT_FOUND)

            if (order.userId.value != userId) {
                throw ValidationException(Message.Orders.UNAUTHORIZED)
            }
            if (order.status !in listOf(com.piashcse.constants.OrderStatus.DELIVERED, com.piashcse.constants.OrderStatus.RECEIVED)) {
                throw ValidationException("Refunds can only be requested after delivery")
            }
            if (order.paymentStatus != PaymentStatus.COMPLETED) {
                throw ValidationException("Refunds can only be requested for paid orders")
            }

            // Lock the item row so two concurrent creates cannot both pass the guard.
            val orderItem =
                OrderItemDAO.find {
                    (OrderItemTable.id eq request.orderItemId.entityID(OrderItemTable)) and
                        (OrderItemTable.orderId eq orderId.entityID(OrderTable))
                }.forUpdate().firstOrNull() ?: throw ValidationException(Message.Refunds.ITEM_NOT_FOUND)

            if (request.quantity > orderItem.quantity) {
                throw ValidationException("Refund quantity cannot exceed the ordered quantity (${orderItem.quantity})")
            }

            val existingRefund =
                RefundRequestDAO.find {
                    (RefundRequestTable.orderItemId eq orderItem.id) and
                        (RefundRequestTable.status inList liveStatuses())
                }.firstOrNull()
            if (existingRefund != null) {
                throw ValidationException(Message.Refunds.ALREADY_EXISTS)
            }
            val alreadyRefundedQuantity =
                RefundRequestDAO.find {
                    (RefundRequestTable.orderItemId eq orderItem.id) and
                        (RefundRequestTable.status eq RefundStatus.REFUNDED)
                }.fold(0) { acc, row -> acc + row.quantity }
            if (alreadyRefundedQuantity + request.quantity > orderItem.quantity) {
                throw ValidationException(
                    "Refund quantity exceeds the remaining ordered quantity " +
                        "(${orderItem.quantity - alreadyRefundedQuantity})",
                )
            }

            val refundRequest =
                RefundRequestDAO.new {
                    this.orderItemId = orderItem.id
                    this.userId = userId.entityID(UserTable)
                    this.orderId = orderId.entityID(OrderTable)
                    this.reason = request.reason
                    this.images = request.images
                    this.currency = order.currency
                    this.quantity = request.quantity
                    this.status = RefundStatus.PENDING
                }
            eventFor(refundRequest, "NONE", RefundStatus.PENDING.name)
            refundRequest.toRefundRequestResponse()
        }

    override suspend fun getRefundsByOrderId(
        orderId: String,
        userId: String,
        userType: UserType,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RefundRequestResponse> = RefundQueries.getRefundsByOrderId(orderId, userId, userType, limit, offset)

    override suspend fun getRefundById(
        refundId: String,
        userId: String,
        userType: UserType,
    ): RefundRequestResponse? = RefundQueries.getRefundById(refundId, userId, userType)

    override suspend fun addRefundEvidence(
        refundId: String,
        userId: String,
        imageUrl: String,
    ): RefundRequestResponse =
        query {
            val refund = RefundRequestDAO.findById(refundId) ?: throw ValidationException(Message.Refunds.NOT_FOUND)
            if (refund.userId.value != userId) throw ForbiddenException(Message.Errors.FORBIDDEN)
            if (refund.status !in listOf(RefundStatus.PENDING, RefundStatus.APPROVED)) {
                throw ValidationException("Evidence can only be added before the refund is resolved")
            }
            val existing = refund.images?.takeIf { it.isNotBlank() }?.split(",")?.toMutableList() ?: mutableListOf()
            if (imageUrl !in existing) existing.add(imageUrl)
            if (existing.size > 8) throw ValidationException("At most 8 evidence images are allowed")
            refund.images = existing.joinToString(",")
            refund.toRefundRequestResponse()
        }

    override suspend fun updateRefundStatus(
        refundId: String,
        request: UpdateRefundStatusRequest,
        userId: String,
    ): RefundRequestResponse {
        val providerRefund = provider.prepare(refundId, request, userId)
        val result =
            query {
                val refundReq =
                    RefundRequestDAO.find { RefundRequestTable.id eq refundId.entityID(RefundRequestTable) }
                        .forUpdate()
                        .firstOrNull()
                        ?: throw ValidationException(Message.Refunds.NOT_FOUND)

                provider.requireActor(refundReq, userId)
                if (request.status == RefundStatus.REFUNDED) provider.requireMoneyAuthority(userId)
                if (!provider.canTransitionTo(refundReq.status, request.status)) {
                    throw ConflictException(Message.Refunds.invalidTransition(refundReq.status.name, request.status.name))
                }

                val order =
                    OrderDAO.findById(refundReq.orderId.value)
                        ?: throw ValidationException(Message.Orders.NOT_FOUND)
                val orderItem =
                    OrderItemDAO.findById(refundReq.orderItemId.value)
                        ?: throw ValidationException(Message.Refunds.ITEM_NOT_FOUND)
                if (request.status == RefundStatus.REFUNDED &&
                    refundReq.status == RefundStatus.APPROVED &&
                    !orderItem.isDigital &&
                    !request.forceWithoutReturn
                ) {
                    throw ValidationException(
                        "Mark the return as shipped before refunding, or set forceWithoutReturn for a kept/damaged item",
                    )
                }
                val maxRefundAmount = provider.maxRefundableAmount(order, orderItem, refundReq.quantity)
                provider.validateAmounts(order, orderItem, refundReq.quantity, request, maxRefundAmount)
                if (request.status == RefundStatus.REFUNDED && request.refundAmount == null && refundReq.refundAmount == null) {
                    throw ValidationException(Message.Refunds.REFUND_AMOUNT_REQUIRED)
                }

                val fromStatus = refundReq.status
                refundReq.status = request.status
                request.decisionReason?.takeIf { it.isNotBlank() }?.let { refundReq.decisionReason = it }
                if (request.status in listOf(RefundStatus.REFUNDED, RefundStatus.REJECTED)) {
                    refundReq.resolvedAt = currentUtc()
                }
                request.refundAmount?.let { refundReq.refundAmount = it }
                request.refundShippingAmount?.let { refundReq.shippingRefund = it }
                request.refundTaxAmount?.let { refundReq.taxRefund = it }
                request.refundMethod?.let { refundReq.refundMethod = it }

                val providerPending =
                    providerRefund != null &&
                        providerRefund.status?.lowercase() !in setOf("succeeded", "refunded", "completed")
                if (request.status == RefundStatus.REFUNDED && providerPending) {
                    // The provider has accepted the refund but has not settled
                    // it. Keep the local request open; the signed webhook
                    // reconciles it to REFUNDED when the money actually moves.
                    refundReq.status = fromStatus
                    refundReq.resolvedAt = null
                } else if (request.status == RefundStatus.REFUNDED) {
                    RefundEffects.applyRefunded(refundReq, order, orderItem, maxRefundAmount)
                }
                providerRefund?.let {
                    refundReq.providerRefundId = it.providerRefundId
                    refundReq.providerRefundStatus = it.status
                }
                if (request.status == RefundStatus.REJECTED) AppMetrics.refundsRejected.increment()
                eventFor(refundReq, fromStatus.name, refundReq.status.name)
                refundReq.toRefundRequestResponse()
            }
        CatalogCache.invalidate()
        return result
    }

    override suspend fun reconcileProviderRefund(
        providerRefundId: String,
        status: String,
        amount: BigDecimal?,
    ): Boolean = provider.reconcile(providerRefundId, status, amount)

    override suspend fun shipRefund(
        refundId: String,
        request: ShipRefundRequest,
        userId: String,
    ): RefundRequestResponse =
        query {
            val refundReq =
                RefundRequestDAO.find { RefundRequestTable.id eq refundId.entityID(RefundRequestTable) }
                    .forUpdate()
                    .firstOrNull()
                    ?: throw ValidationException(Message.Refunds.NOT_FOUND)

            if (refundReq.userId.value != userId) {
                throw ValidationException(Message.Orders.UNAUTHORIZED)
            }

            if (refundReq.status != RefundStatus.APPROVED) {
                throw ValidationException(Message.Refunds.MUST_BE_APPROVED)
            }
            request.trackingNumber.requireNotBlank("Tracking number")

            refundReq.trackingNumber = request.trackingNumber
            refundReq.status = RefundStatus.SHIPPED
            eventFor(refundReq, RefundStatus.APPROVED.name, RefundStatus.SHIPPED.name)
            refundReq.toRefundRequestResponse()
        }

    override suspend fun listForSeller(
        sellerUserId: String,
        status: RefundStatus?,
        search: String?,
        from: java.time.Instant?,
        to: java.time.Instant?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RefundRequestResponse> = RefundQueries.listForSeller(sellerUserId, status, search, from, to, limit, offset)

    override suspend fun listAll(
        status: RefundStatus?,
        search: String?,
        from: java.time.Instant?,
        to: java.time.Instant?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RefundRequestResponse> = RefundQueries.listAll(status, search, from, to, limit, offset)
}
