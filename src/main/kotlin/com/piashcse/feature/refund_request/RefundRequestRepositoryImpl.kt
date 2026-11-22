package com.piashcse.feature.refund_request

import com.piashcse.constants.Message
import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.PayoutStatus
import com.piashcse.constants.RefundMethod
import com.piashcse.constants.RefundStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.*
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.OutboxAggregate
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.RefundStatusChangedEvent
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.order.OrderCancellation
import com.piashcse.feature.order.OrderSearch
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
import com.piashcse.service.CatalogCache
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.emptyPage
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ConflictException
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
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
            OutboxAggregate.REFUND,
            refund.id.value,
            RefundStatusChangedEvent(
                refundId = refund.id.value,
                orderId = refund.orderId.value,
                userId = refund.userId.value,
                email = UserDAO.findById(refund.userId.value)?.email.orEmpty(),
                fromStatus = fromStatus,
                toStatus = toStatus,
                reason = refund.decisionReason,
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
                }.let { enrichRefundPage(it) }
        }

    override suspend fun getRefundById(
        refundId: String,
        userId: String,
        userType: UserType,
    ): RefundRequestResponse? =
        query {
            val refundRequest = RefundRequestDAO.findById(refundId) ?: return@query null

            val order = OrderDAO.findById(refundRequest.orderId.value)
            val item = OrderItemDAO.findById(refundRequest.orderItemId.value)
            val customer = UserDAO.findById(refundRequest.userId.value)
            requireRefundAccess(
                order?.userId?.value ?: refundRequest.userId.value,
                order?.shopId?.value,
                userId,
                userType,
            )

            refundRequest.toRefundRequestResponse(
                orderNumber = order?.orderNumber,
                shopId = order?.shopId?.value,
                customerEmail = customer?.email,
                productName = item?.productName,
            )
        }

    private fun maxRefundableAmount(
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

    private fun restoreRefundEffects(
        refundId: String,
        order: OrderDAO,
        orderItem: OrderItemDAO,
        refundAmount: BigDecimal,
        quantity: Int,
        shippingRefund: BigDecimal,
        taxRefund: BigDecimal,
    ) {
        // Stock and sales counters. Restore each warehouse allocation that is
        // still outstanding for this order line; fall back to the pickup/first
        // warehouse for reservations created before allocation tracking.
        ProductDAO.findById(orderItem.productId.value)?.takeUnless { it.isDigital }?.let { product ->
            val reservations =
                StockReservationDAO.find { StockReservationTable.orderItemId eq orderItem.id }
                    .orderBy(StockReservationTable.createdAt to SortOrder.ASC)
                    .toList()
            var remaining = quantity
            reservations.forEach { reservation ->
                val available = (reservation.quantity - reservation.restoredQuantity).coerceAtLeast(0)
                val take = minOf(available, remaining)
                if (take > 0) {
                    product.restoreStockFor(
                        orderItem.variantId?.value,
                        take,
                        reservation.warehouseId?.value,
                    )
                    reservation.restoredQuantity += take
                    remaining -= take
                }
            }
            if (remaining > 0) {
                product.restoreStockFor(orderItem.variantId?.value, remaining, order.pickupWarehouseId?.value)
            }
            product.removeSales(quantity)
        }

        // Seller totals and payout ledger. Seller-funded merchandise is
        // independent of how the customer paid (wallet/gift are platform
        // instruments and must not be clawed back from the seller).
        val inclusiveTaxShare =
            if (orderItem.taxInclusive) {
                orderItem.taxAmount
                    .multiply(BigDecimal(quantity))
                    .divide(BigDecimal(orderItem.quantity), 2, java.math.RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO.setScale(2)
            }
        val sellerRefundAmount =
            orderItem.total
                .subtract(orderItem.discountAmount)
                .subtract(inclusiveTaxShare)
                .multiply(BigDecimal(quantity))
                .divide(BigDecimal(orderItem.quantity), 2, java.math.RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
        SellerDAO.find { SellerTable.shopId eq order.shopId }.firstOrNull()?.let { seller ->
            val fallbackRate = CommissionService().resolveRateTx(seller, null)
            var totalCommissionReversed = BigDecimal.ZERO
            SellerPayoutDAO.find { SellerPayoutTable.orderId eq order.id }.forEach { payout ->
                // Use the rate snapshot from the original payout; current rules may have changed.
                val rate = payout.commissionRate ?: fallbackRate
                val commissionShare = Money.commission(sellerRefundAmount, rate)
                // The seller only loses the net share of the refunded item; the
                // platform returns its commission on that portion.
                val netRefund = sellerRefundAmount.subtract(commissionShare).max(BigDecimal.ZERO)
                if (payout.status == PayoutStatus.PAID.name) {
                    // Money already left the platform; create a recoverable debt
                    // instead of silently clamping the paid row to zero.
                    SellerBalanceAdjustmentDAO.new {
                        this.sellerId = seller.id
                        this.orderId = order.id
                        amount = netRefund.negate()
                        currency = order.currency
                        reason = "refund:${order.id.value}"
                    }
                } else {
                    payout.payoutAmount = payout.payoutAmount.subtract(netRefund).max(BigDecimal.ZERO)
                    payout.commissionAmount = payout.commissionAmount.subtract(commissionShare).max(BigDecimal.ZERO)
                    if (payout.payoutAmount.compareTo(BigDecimal.ZERO) == 0) {
                        payout.status = PayoutStatus.REFUNDED.name
                    }
                }
                totalCommissionReversed = totalCommissionReversed.add(commissionShare)
            }
            seller.totalSales = seller.totalSales.subtract(sellerRefundAmount).max(BigDecimal.ZERO)
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
            this.transactionId = "refund:$refundId"
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
                // Money authority must be checked before the provider call. A
                // seller may approve/ship a refund but only an admin may move
                // the actual money.
                requireRefundMoneyAuthority(userId)

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
        val providerRefund = prepareProviderRefund(refundId, request, userId)
        val result =
            query {
                val refundReq =
                    RefundRequestDAO.find { RefundRequestTable.id eq refundId.entityID(RefundRequestTable) }
                        .forUpdate()
                        .firstOrNull()
                        ?: throw ValidationException(Message.Refunds.NOT_FOUND)

                requireRefundActor(refundReq, userId)
                if (request.status == RefundStatus.REFUNDED) requireRefundMoneyAuthority(userId)
                if (!canTransitionTo(refundReq.status, request.status)) {
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
                val maxRefundAmount = maxRefundableAmount(order, orderItem, refundReq.quantity)
                validateRefundAmounts(order, orderItem, refundReq.quantity, request, maxRefundAmount)
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
                    applyRefunded(refundReq, order, orderItem, maxRefundAmount)
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

    private fun requireRefundMoneyAuthority(userId: String) {
        val user = UserDAO.findById(userId) ?: throw ValidationException(Message.Errors.NOT_FOUND)
        if (user.userType == UserType.SELLER) {
            throw ForbiddenException("Only an administrator can mark a refund as paid")
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
            refund.id.value,
            order,
            orderItem,
            amount,
            refund.quantity,
            refund.shippingRefund,
            refund.taxRefund,
        )

        // Full refund is decided by quantities, not a cash sum. Wallet/gift
        // payments make the cash total smaller than order.total even when every
        // unit has been returned.
        val refundedByItem =
            RefundRequestDAO.find {
                (RefundRequestTable.orderId eq order.id) and
                    (RefundRequestTable.status eq RefundStatus.REFUNDED)
            }.groupBy { it.orderItemId.value }
                .mapValues { (_, rows) -> rows.sumOf { it.quantity } }
        val fullyRefunded =
            OrderItemDAO.itemsForOrder(order.id).all { item ->
                (refundedByItem[item.id.value] ?: 0) >= item.quantity
            }
        if (!fullyRefunded && order.subTotal > BigDecimal.ZERO && orderItem.quantity > 0) {
            val valueRatio =
                orderItem.total.divide(order.subTotal, 8, java.math.RoundingMode.HALF_UP)
            val qtyRatio =
                BigDecimal(refund.quantity)
                    .divide(BigDecimal(orderItem.quantity), 8, java.math.RoundingMode.HALF_UP)
            val fx = order.fxRate.takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE
            val walletUsd =
                order.walletDiscount
                    .multiply(valueRatio)
                    .multiply(qtyRatio)
                    .divide(fx, 2, java.math.RoundingMode.HALF_UP)
            val giftUsd =
                order.giftCardDiscount
                    .multiply(valueRatio)
                    .multiply(qtyRatio)
                    .divide(fx, 2, java.math.RoundingMode.HALF_UP)
            LoyaltyService().restorePartialInstrumentsTx(
                order.userId.value,
                order.id.value,
                walletUsd,
                giftUsd,
                refund.id.value,
            )
        }
        if (order.paymentStatus == PaymentStatus.COMPLETED && fullyRefunded) {
            order.paymentStatus = PaymentStatus.REFUNDED
            PaymentDAO.find {
                (PaymentTable.orderId eq order.id) and (PaymentTable.status eq PaymentStatus.COMPLETED)
            }.forEach { it.status = PaymentStatus.REFUNDED }
            OrderCancellation.reverseCouponUsageForOrder(order.id.value)
            PromotionService().reverseForOrderTx(order.id.value)
            LoyaltyService().reverseOrderTx(order.userId.value, order.id.value)
            DigitalDeliveryService().revokeOrderTx(order.id.value)
        }
    }

    private fun canTransitionTo(
        current: RefundStatus,
        target: RefundStatus,
    ): Boolean = RefundStatus.canTransitionTo(current, target)

    override suspend fun reconcileProviderRefund(
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
            applyRefunded(refund, order, orderItem, maxRefundAmount)
            eventFor(refund, fromStatus.name, RefundStatus.REFUNDED.name)
            true
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

    override suspend fun listForSeller(
        sellerUserId: String,
        status: RefundStatus?,
        search: String?,
        from: java.time.Instant?,
        to: java.time.Instant?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RefundRequestResponse> =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId ?: throw ValidationException("Seller has no shop")
            var orderIds = OrderDAO.find { OrderTable.shopId eq shopId }.map { it.id }
            search?.takeIf { it.isNotBlank() }?.let { term ->
                val pattern = "%${term.trim().lowercase()}%"
                orderIds =
                    OrderDAO.find {
                        (OrderTable.id inList orderIds) and
                            ((OrderTable.orderNumber.lowerCase() like pattern) or (OrderTable.contactEmail.lowerCase() like pattern))
                    }.map { it.id }
            }
            if (orderIds.isEmpty()) {
                return@query emptyPage(limit, offset)
            }
            RefundRequestTable.selectAll()
                .where { RefundRequestTable.orderId inList orderIds }
                .also { q ->
                    status?.let { q.andWhere { RefundRequestTable.status eq it } }
                    from?.let { date ->
                        q.andWhere {
                            RefundRequestTable.createdAt greaterEq
                                java.time.LocalDateTime.ofInstant(date, java.time.ZoneOffset.UTC)
                        }
                    }
                    to?.let { date ->
                        q.andWhere {
                            RefundRequestTable.createdAt lessEq
                                java.time.LocalDateTime.ofInstant(date, java.time.ZoneOffset.UTC)
                        }
                    }
                }.orderBy(RefundRequestTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    RefundRequestDAO.wrapRow(it).toRefundRequestResponse()
                }.let { enrichRefundPage(it) }
        }

    override suspend fun listAll(
        status: RefundStatus?,
        search: String?,
        from: java.time.Instant?,
        to: java.time.Instant?,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<RefundRequestResponse> =
        readQuery {
            val matchingOrderIds = OrderSearch.matchingOrderIdsTx(search)
            if (matchingOrderIds != null && matchingOrderIds.isEmpty()) {
                return@readQuery emptyPage(limit, offset)
            }
            RefundRequestTable.selectAll()
                .also { q ->
                    status?.let { q.andWhere { RefundRequestTable.status eq it } }
                    matchingOrderIds?.let { q.andWhere { RefundRequestTable.orderId inList it } }
                    from?.let { date ->
                        q.andWhere {
                            RefundRequestTable.createdAt greaterEq
                                java.time.LocalDateTime.ofInstant(date, java.time.ZoneOffset.UTC)
                        }
                    }
                    to?.let { date ->
                        q.andWhere {
                            RefundRequestTable.createdAt lessEq
                                java.time.LocalDateTime.ofInstant(date, java.time.ZoneOffset.UTC)
                        }
                    }
                }.orderBy(RefundRequestTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) {
                    RefundRequestDAO.wrapRow(it).toRefundRequestResponse()
                }.let { enrichRefundPage(it) }
        }

    private fun enrichRefundPage(page: PaginatedResponse<RefundRequestResponse>): PaginatedResponse<RefundRequestResponse> {
        if (page.data.isEmpty()) return page
        val orders =
            OrderDAO.find { OrderTable.id inList page.data.map { it.orderId }.distinct().map { it.entityID(OrderTable) } }
                .associateBy { it.id.value }
        val items =
            OrderItemDAO.find {
                OrderItemTable.id inList page.data.map { it.orderItemId }.distinct().map { it.entityID(OrderItemTable) }
            }.associateBy { it.id.value }
        val users =
            UserDAO.find { UserTable.id inList page.data.map { it.userId }.distinct().map { it.entityID(UserTable) } }
                .associateBy { it.id.value }
        return page.copy(
            data =
                page.data.map { refund ->
                    val order = orders[refund.orderId]
                    refund.copy(
                        orderNumber = order?.orderNumber,
                        shopId = order?.shopId?.value,
                        customerEmail = users[refund.userId]?.email,
                        productName = items[refund.orderItemId]?.productName,
                    )
                },
        )
    }
}
