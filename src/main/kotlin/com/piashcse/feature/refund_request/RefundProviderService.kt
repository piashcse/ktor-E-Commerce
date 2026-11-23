package com.piashcse.feature.refund_request

import com.piashcse.constants.Message
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.RefundMethod
import com.piashcse.constants.RefundStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.feature.payment.PaymentGateway
import com.piashcse.model.request.UpdateRefundStatusRequest
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import java.math.BigDecimal

/** Original-instrument refund execution and provider reconciliation. */
class RefundProviderService(
    private val gateway: PaymentGateway,
) {
    data class ProviderRefundPlan(
        val providerReference: String,
        val amount: BigDecimal,
        val currency: String,
    )

    data class ProviderRefundResult(
        val providerRefundId: String?,
        val status: String?,
    )

    fun maxRefundableAmount(
        order: OrderDAO,
        orderItem: OrderItemDAO,
        quantity: Int,
    ): BigDecimal {
        // Use the discount actually persisted on this line. Applying the whole
        // order discount proportionally over-refunds ineligible lines and
        // under-refunds discounted ones when a coupon is item-scoped. Wallet
        // credit/gift-card amounts are payment instruments, not item discounts,
        // so they are subtracted from the cash-refund cap separately.
        val instrumentDiscount = order.walletDiscount.add(order.giftCardDiscount)
        val instrumentShare =
            if (order.subTotal > BigDecimal.ZERO) {
                Money.proportionalSplit(instrumentDiscount, orderItem.total, order.subTotal)
            } else {
                BigDecimal.ZERO.setScale(2)
            }
        val netItemTotal =
            orderItem.total
                .subtract(orderItem.discountAmount)
                .subtract(instrumentShare)
                .max(BigDecimal.ZERO)
        val unitNet = netItemTotal.divide(BigDecimal(orderItem.quantity), 4, java.math.RoundingMode.HALF_UP)
        return unitNet.multiply(BigDecimal(quantity)).setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Returns money to the original card when the request asks for an ORIGINAL
     * refund and the order has a completed provider payment. Validation and the
     * actor check run before the network call so an unauthorized/invalid request
     * never reaches the processor.
     */
    suspend fun prepare(
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
                requireActor(refund, userId)
                // Money authority must be checked before the provider call. A
                // seller may approve/ship a refund but only an admin may move
                // the actual money.
                requireMoneyAuthority(userId)

                val order =
                    OrderDAO.findById(refund.orderId.value)
                        ?: throw ValidationException(Message.Orders.NOT_FOUND)
                val orderItem =
                    OrderItemDAO.findById(refund.orderItemId.value)
                        ?: throw ValidationException(Message.Refunds.ITEM_NOT_FOUND)
                if (refund.status == RefundStatus.APPROVED &&
                    !orderItem.isDigital &&
                    !request.forceWithoutReturn
                ) {
                    throw ValidationException(
                        "Mark the return as shipped before refunding, or set forceWithoutReturn for a kept/damaged item",
                    )
                }
                val maxRefundAmount = maxRefundableAmount(order, orderItem, refund.quantity)
                validateAmounts(order, orderItem, refund.quantity, request, maxRefundAmount)
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
                // Mark the attempt before the network call. If the process/DB
                // fails after money moves, a retry reuses the same gateway
                // idempotency key.
                refund.providerRefundStatus = "processing"
                ProviderRefundPlan(
                    providerReference =
                        payment.providerReference
                            ?: throw ValidationException("Completed payment has no provider reference"),
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

    fun requireMoneyAuthority(userId: String) {
        val user = UserDAO.findById(userId) ?: throw ValidationException(Message.Errors.NOT_FOUND)
        if (user.userType == UserType.SELLER) {
            throw ForbiddenException("Only an administrator can mark a refund as paid")
        }
    }

    fun requireActor(
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

    fun validateAmounts(
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

    fun canTransitionTo(
        current: RefundStatus,
        target: RefundStatus,
    ): Boolean = RefundStatus.canTransitionTo(current, target)

    suspend fun reconcile(
        providerRefundId: String,
        status: String,
        amount: BigDecimal?,
    ): Boolean =
        query {
            val refund =
                RefundRequestDAO.find { RefundRequestTable.providerRefundId eq providerRefundId }
                    .forUpdate()
                    .firstOrNull() ?: return@query false
            refund.providerRefundStatus = status
            val succeeded = status.lowercase() in setOf("succeeded", "refunded", "completed")
            if (!succeeded || refund.status == RefundStatus.REFUNDED) return@query true

            val order =
                OrderDAO.findById(refund.orderId.value)
                    ?: throw ValidationException(Message.Orders.NOT_FOUND)
            val orderItem =
                OrderItemDAO.findById(refund.orderItemId.value)
                    ?: throw ValidationException(Message.Refunds.ITEM_NOT_FOUND)
            val maxRefundAmount = maxRefundableAmount(order, orderItem, refund.quantity)
            val refundAmount = refund.refundAmount ?: maxRefundAmount
            val expected =
                refundAmount.add(refund.shippingRefund).add(refund.taxRefund).setScale(2)
            if (amount != null && amount.compareTo(expected) != 0) {
                refund.providerRefundStatus = "amount_mismatch:$status"
                return@query true
            }
            if (refund.refundMethod == null) refund.refundMethod = RefundMethod.ORIGINAL
            refund.refundAmount = refundAmount
            val fromStatus = refund.status
            refund.status = RefundStatus.REFUNDED
            refund.resolvedAt = currentUtc()
            RefundEffects.applyRefunded(refund, order, orderItem, maxRefundAmount)
            eventFor(refund, fromStatus.name, RefundStatus.REFUNDED.name)
            true
        }
}
