package com.piashcse.feature.refund_request

import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.PayoutStatus
import com.piashcse.constants.RefundStatus
import com.piashcse.database.entities.*
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.order.OrderCancellation
import com.piashcse.feature.promotion.PromotionService
import com.piashcse.feature.seller.CommissionService
import com.piashcse.feature.seller.SellerTotals
import com.piashcse.feature.subscription.DigitalDeliveryService
import com.piashcse.service.AppMetrics
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import java.math.BigDecimal

/** Stock, seller, payout and instrument side effects of a completed refund. */
internal object RefundEffects {
    fun applyRefunded(
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
            SellerTotals.reversePartialTx(seller, sellerRefundAmount, totalCommissionReversed)
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
}
