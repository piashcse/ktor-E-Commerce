package com.piashcse.feature.refund_request

import com.piashcse.constants.Message
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RefundMethod
import com.piashcse.constants.RefundStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.RefundStatusChangedEvent
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.order.OrderCancellation
import com.piashcse.feature.payment.ManualPaymentGateway
import com.piashcse.feature.payment.PaymentGateway
import com.piashcse.feature.promotion.PromotionService
import com.piashcse.feature.seller.CommissionService
import com.piashcse.feature.subscription.DigitalDeliveryService
import com.piashcse.mapper.toRefundRequestResponse
import com.piashcse.model.request.RefundRequestRequest
import com.piashcse.model.request.ShipRefundRequest
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.model.response.RefundRequestResponse
import com.piashcse.service.AppMetrics
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal

class RefundRequestRepositoryImpl(
    private val gateway: PaymentGateway = ManualPaymentGateway(),
) : RefundRequestRepository {
    private data class ProviderRefundPlan(
        val providerReference: String,
        val amount: BigDecimal,
        val currency: String,
    )

    private data class ProviderRefundResult(
        val providerRefundId: String?,
        val status: String?,
    )

    private fun liveStatuses() = listOf(RefundStatus.PENDING, RefundStatus.APPROVED, RefundStatus.SHIPPED)

    private fun eventFor(
        refund: RefundRequestDAO,
        fromStatus: String,
        toStatus: String,
    ) {
        OutboxPublisher.enqueueTx(
            "REFUND",
            refund.id.value,
            RefundStatusChangedEvent(
                refundId = refund.id.value,
                orderId = refund.orderId.value,
                userId = refund.userId.value,
                email = UserDAO.findById(refund.userId.value)?.email.orEmpty(),
                fromStatus = fromStatus,
                toStatus = toStatus,
            ),
        )
    }

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
    ): PaginatedResponse<RefundRequestResponse> =
        query {
            val order =
                OrderDAO.findById(orderId)
                    ?: throw ValidationException(Message.Orders.NOT_FOUND)

            requireRefundAccess(order.userId.value, order.shopId?.value, userId, userType)

            RefundRequestTable.selectAll()
                .andWhere { RefundRequestTable.orderId eq orderId.entityID(OrderTable) }
                .orderBy(RefundRequestTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    RefundRequestDAO.wrapRow(it).toRefundRequestResponse()
                }
        }

    override suspend fun getRefundById(
        refundId: String,
        userId: String,
        userType: UserType,
    ): RefundRequestResponse? =
        query {
            val refundRequest = RefundRequestDAO.findById(refundId) ?: return@query null

            val order = OrderDAO.findById(refundRequest.orderId.value)
            requireRefundAccess(
                order?.userId?.value ?: refundRequest.userId.value,
                order?.shopId?.value,
                userId,
                userType,
            )

            refundRequest.toRefundRequestResponse()
        }

    private fun maxRefundableAmount(
        order: OrderDAO,
        orderItem: OrderItemDAO,
        quantity: Int,
    ): BigDecimal {
        val discountShare =
            if (order.subTotal > BigDecimal.ZERO) {
                Money.proportionalSplit(order.discountAmount, orderItem.total, order.subTotal)
            } else {
                BigDecimal.ZERO.setScale(2)
            }
        val netItemTotal = orderItem.total.subtract(discountShare).max(BigDecimal.ZERO)
        val unitNet = netItemTotal.divide(BigDecimal(orderItem.quantity), 4, java.math.RoundingMode.HALF_UP)
        return unitNet.multiply(BigDecimal(quantity)).setScale(2, java.math.RoundingMode.HALF_UP)
    }

    private fun restoreRefundEffects(
        order: OrderDAO,
        orderItem: OrderItemDAO,
        refundAmount: BigDecimal,
        quantity: Int,
        shippingRefund: BigDecimal,
        taxRefund: BigDecimal,
    ) {
        // Stock and sales counters.
        ProductDAO.findById(orderItem.productId.value)?.let { product ->
            product.restoreStockFor(orderItem.variantId?.value, quantity, order.pickupWarehouseId?.value)
            product.removeSales(quantity)
        }

        // Seller totals and payout ledger.
        SellerDAO.find { SellerTable.shopId eq order.shopId }.firstOrNull()?.let { seller ->
            val fallbackRate = CommissionService().resolveRateTx(seller, null)
            var totalCommissionReversed = BigDecimal.ZERO
            SellerPayoutDAO.find { SellerPayoutTable.orderId eq order.id }.forEach { payout ->
                // Use the rate snapshot from the original payout; current rules may have changed.
                val rate = payout.commissionRate ?: fallbackRate
                val commissionShare = Money.commission(refundAmount, rate)
                // The seller only loses the net share of the refunded item; the
                // platform returns its commission on that portion.
                val netRefund = refundAmount.subtract(commissionShare).max(BigDecimal.ZERO)
                payout.payoutAmount = payout.payoutAmount.subtract(netRefund).max(BigDecimal.ZERO)
                payout.commissionAmount = payout.commissionAmount.subtract(commissionShare).max(BigDecimal.ZERO)
                totalCommissionReversed = totalCommissionReversed.add(commissionShare)
                if (payout.payoutAmount.compareTo(BigDecimal.ZERO) == 0) {
                    payout.status = "REFUNDED"
                }
            }
            seller.totalSales = seller.totalSales.subtract(refundAmount).max(BigDecimal.ZERO)
            seller.totalCommission = seller.totalCommission.subtract(totalCommissionReversed).max(BigDecimal.ZERO)
        }

        // Refund ledger entry so the original payment is no longer the last word.
        val ledgerAmount = refundAmount.add(shippingRefund).add(taxRefund).setScale(2)
        PaymentDAO.new {
            this.orderId = order.id
            this.userId = order.userId
            this.amount = ledgerAmount
            this.currency = order.currency
            this.status = PaymentStatus.REFUNDED
            this.paymentMethod = order.paymentMethod ?: PaymentMethod.COD
            this.transactionId = "refund:${order.id.value}:${orderItem.id.value}:${ledgerAmount.toPlainString()}"
        }
    }

    /**
     * Returns money to the original card when the request asks for an ORIGINAL
     * refund and the order has a completed provider payment. Validation and the
     * actor check run before the network call so an unauthorized/invalid request
     * never reaches the processor.
     */
    private suspend fun prepareProviderRefund(
        refundId: String,
        request: UpdateRefundStatusRequest,
        userId: String,
    ): ProviderRefundResult? {
        if (request.status != RefundStatus.REFUNDED) return null
        val plan =
            query {
                val refund =
                    RefundRequestDAO.find { RefundRequestTable.id eq refundId.entityID(RefundRequestTable) }
                        .firstOrNull() ?: return@query null
                if (refund.status == RefundStatus.REFUNDED || refund.providerRefundId != null) return@query null
                val method = request.refundMethod ?: refund.refundMethod
                if (method != RefundMethod.ORIGINAL) return@query null
                if (!canTransitionTo(refund.status, request.status)) return@query null
                requireRefundActor(refund, userId)

                val order =
                    OrderDAO.findById(refund.orderId.value)
                        ?: throw ValidationException(Message.Orders.NOT_FOUND)
                val orderItem =
                    OrderItemDAO.findById(refund.orderItemId.value)
                        ?: throw ValidationException(Message.Refunds.ITEM_NOT_FOUND)
                val maxRefundAmount = maxRefundableAmount(order, orderItem, refund.quantity)
                validateRefundAmounts(order, orderItem, refund.quantity, request, maxRefundAmount)
                if (request.refundAmount == null && refund.refundAmount == null) {
                    // Fail before the provider call: updateRefundStatus would
                    // otherwise reject the same request after money has moved.
                    throw ValidationException(Message.Refunds.REFUND_AMOUNT_REQUIRED)
                }

                val payment =
                    PaymentDAO.find {
                        (PaymentTable.orderId eq refund.orderId) and
                            (PaymentTable.status eq PaymentStatus.COMPLETED) and
                            (PaymentTable.providerReference.isNotNull())
                    }.orderBy(PaymentTable.createdAt to SortOrder.DESC)
                        .firstOrNull() ?: return@query null

                val amount =
                    (request.refundAmount ?: refund.refundAmount ?: maxRefundAmount)
                        .add(request.refundShippingAmount ?: refund.shippingRefund)
                        .add(request.refundTaxAmount ?: refund.taxRefund)
                ProviderRefundPlan(
                    providerReference = payment.providerReference!!,
                    amount = amount,
                    currency = order.currency,
                )
            } ?: return null

        val result = gateway.refund(plan.providerReference, plan.amount, plan.currency, "refund:$refundId")
        if (!result.success) {
            throw ValidationException(result.message ?: "Provider refund failed")
        }
        return ProviderRefundResult(result.providerRefundId, result.status)
    }

    override suspend fun updateRefundStatus(
        refundId: String,
        request: UpdateRefundStatusRequest,
        userId: String,
    ): RefundRequestResponse {
        val providerRefund = prepareProviderRefund(refundId, request, userId)
        return query {
            val refundReq =
                RefundRequestDAO.find { RefundRequestTable.id eq refundId.entityID(RefundRequestTable) }
                    .forUpdate()
                    .firstOrNull()
                    ?: throw ValidationException(Message.Refunds.NOT_FOUND)

            requireRefundActor(refundReq, userId)
            if (!canTransitionTo(refundReq.status, request.status)) {
                throw ValidationException(Message.Refunds.invalidTransition(refundReq.status.name, request.status.name))
            }

            val order =
                OrderDAO.findById(refundReq.orderId.value)
                    ?: throw ValidationException(Message.Orders.NOT_FOUND)
            val orderItem =
                OrderItemDAO.findById(refundReq.orderItemId.value)
                    ?: throw ValidationException(Message.Refunds.ITEM_NOT_FOUND)
            val maxRefundAmount = maxRefundableAmount(order, orderItem, refundReq.quantity)
            validateRefundAmounts(order, orderItem, refundReq.quantity, request, maxRefundAmount)
            if (request.status == RefundStatus.REFUNDED && request.refundAmount == null && refundReq.refundAmount == null) {
                throw ValidationException(Message.Refunds.REFUND_AMOUNT_REQUIRED)
            }

            val fromStatus = refundReq.status
            refundReq.status = request.status
            if (request.status in listOf(RefundStatus.REFUNDED, RefundStatus.REJECTED)) {
                refundReq.resolvedAt = currentUtc()
            }
            request.refundAmount?.let { refundReq.refundAmount = it }
            request.refundShippingAmount?.let { refundReq.shippingRefund = it }
            request.refundTaxAmount?.let { refundReq.taxRefund = it }
            request.refundMethod?.let { refundReq.refundMethod = it }

            if (request.status == RefundStatus.REFUNDED) {
                applyRefunded(refundReq, order, orderItem, maxRefundAmount)
            }
            providerRefund?.let {
                refundReq.providerRefundId = it.providerRefundId
                refundReq.providerRefundStatus = it.status
            }
            if (request.status == RefundStatus.REJECTED) AppMetrics.refundsRejected.increment()
            eventFor(refundReq, fromStatus.name, request.status.name)
            refundReq.toRefundRequestResponse()
        }
    }

    private fun requireRefundActor(
        refund: RefundRequestDAO,
        userId: String,
    ) {
        val user = UserDAO.findById(userId) ?: throw ValidationException(Message.Errors.NOT_FOUND)
        val isAdmin = user.userType in listOf(UserType.ADMIN, UserType.SUPER_ADMIN)
        val seller = findSellerByUserId(userId)
        if (!isAdmin && seller == null) {
            throw ForbiddenException(Message.Errors.FORBIDDEN)
        }
        if (seller != null) {
            val order =
                OrderDAO.findById(refund.orderId.value)
                    ?: throw ValidationException(Message.Orders.NOT_FOUND)
            val orderShopId = order.shopId?.value
            if (orderShopId == null || !sellerOwnsShop(seller, orderShopId)) {
                throw ForbiddenException(Message.Errors.FORBIDDEN)
            }
        }
    }

    private fun validateRefundAmounts(
        order: OrderDAO,
        orderItem: OrderItemDAO,
        refundQuantity: Int,
        request: UpdateRefundStatusRequest,
        maxRefundAmount: BigDecimal,
    ) {
        request.refundShippingAmount?.let { amount ->
            val alreadyRefundedShipping =
                RefundRequestDAO.find {
                    (RefundRequestTable.orderId eq order.id) and
                        (RefundRequestTable.status eq RefundStatus.REFUNDED)
                }.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.shippingRefund) }
            val remainingShipping = order.shippingCost.subtract(alreadyRefundedShipping).max(BigDecimal.ZERO)
            if (amount < BigDecimal.ZERO || amount > remainingShipping) {
                throw ValidationException("Shipping refund cannot exceed the remaining order shipping cost")
            }
        }
        request.refundTaxAmount?.let { amount ->
            // Inclusive lines already contain tax in the item total; a separate tax refund would double-pay.
            if (orderItem.taxInclusive && amount > BigDecimal.ZERO) {
                throw ValidationException("Tax-inclusive items cannot be given a separate tax refund")
            }
            val itemTaxShare =
                if (orderItem.taxInclusive || orderItem.quantity == 0) {
                    BigDecimal.ZERO
                } else {
                    orderItem.taxAmount
                        .multiply(BigDecimal(refundQuantity))
                        .divide(BigDecimal(orderItem.quantity), 2, java.math.RoundingMode.HALF_UP)
                }
            val alreadyRefundedTax =
                RefundRequestDAO.find {
                    (RefundRequestTable.orderItemId eq orderItem.id) and
                        (RefundRequestTable.status eq RefundStatus.REFUNDED)
                }.fold(BigDecimal.ZERO) { acc, row -> acc.add(row.taxRefund) }
            if (amount < BigDecimal.ZERO || amount > itemTaxShare.subtract(alreadyRefundedTax).max(BigDecimal.ZERO)) {
                throw ValidationException("Tax refund cannot exceed the remaining tax on this item")
            }
        }
        request.refundAmount?.let { amount ->
            if (amount <= BigDecimal.ZERO || amount.stripTrailingZeros().scale() > 2) {
                throw ValidationException(Message.Refunds.AMOUNT_EXCEEDS_ITEM_TOTAL)
            }
            if (amount < BigDecimal("0.01")) {
                throw ValidationException("Refund amount must be at least 0.01")
            }
            if (amount > maxRefundAmount) {
                throw ValidationException(Message.Refunds.AMOUNT_EXCEEDS_ITEM_TOTAL)
            }
        }
    }

    private fun applyRefunded(
        refund: RefundRequestDAO,
        order: OrderDAO,
        orderItem: OrderItemDAO,
        maxRefundAmount: BigDecimal,
    ) {
        AppMetrics.refundsCompleted.increment()
        if (refund.refundMethod == null) {
            throw ValidationException("Refund method is required when marking a refund as REFUNDED")
        }
        val amount = refund.refundAmount ?: maxRefundAmount
        refund.refundAmount = amount
        restoreRefundEffects(
            order,
            orderItem,
            amount,
            refund.quantity,
            refund.shippingRefund,
            refund.taxRefund,
        )

        // Full refund = item refunds + shipping/tax cover the whole order total.
        val refundedTotal =
            RefundRequestDAO.find {
                (RefundRequestTable.orderId eq refund.orderId) and
                    (RefundRequestTable.status eq RefundStatus.REFUNDED)
            }.fold(BigDecimal.ZERO) { acc, row ->
                acc.add(row.refundAmount ?: BigDecimal.ZERO)
                    .add(row.shippingRefund)
                    .add(row.taxRefund)
            }
        if (order.paymentStatus == PaymentStatus.COMPLETED && refundedTotal >= order.total) {
            order.paymentStatus = PaymentStatus.REFUNDED
            OrderCancellation.reverseCouponUsageForOrder(order.id.value)
            PromotionService().reverseForOrderTx(order.id.value)
            LoyaltyService().reverseOrderTx(order.userId.value, order.id.value)
            DigitalDeliveryService().revokeOrderTx(order.id.value)
        }
    }

    private fun canTransitionTo(
        current: RefundStatus,
        target: RefundStatus,
    ): Boolean =
        when (current) {
            RefundStatus.PENDING -> target in listOf(RefundStatus.APPROVED, RefundStatus.REJECTED)
            RefundStatus.APPROVED -> target in listOf(RefundStatus.SHIPPED, RefundStatus.REFUNDED, RefundStatus.REJECTED)
            RefundStatus.SHIPPED -> target == RefundStatus.REFUNDED
            RefundStatus.REJECTED, RefundStatus.REFUNDED -> false
        }

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
}
