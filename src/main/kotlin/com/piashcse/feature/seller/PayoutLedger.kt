package com.piashcse.feature.seller

import com.piashcse.constants.PayoutStatus
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerPayoutDAO
import com.piashcse.database.entities.SellerPayoutTable
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.utils.extension.entityID
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Creates the seller payout ledger row for a fully paid order.
 *
 * Runs inside the payment-completion transaction (and remains available as an
 * idempotent fallback for the outbox subscriber). Doing it transactionally
 * means a refund/cancellation that follows immediately can always reverse the
 * row, instead of racing an asynchronous event consumer.
 */
object PayoutLedger {
    /** Returns the created row, or null when the order has no seller or already has a payout. */
    fun createForOrderTx(order: OrderDAO): SellerPayoutDAO? {
        val shopId = order.shopId?.value ?: return null
        val seller =
            SellerDAO.find { SellerTable.shopId eq shopId.entityID(ShopTable) }.firstOrNull()
                ?: return null
        val exists =
            SellerPayoutDAO.find {
                (SellerPayoutTable.sellerId eq seller.id) and
                    (SellerPayoutTable.orderId eq order.id)
            }.firstOrNull() != null
        if (exists) return null

        // Seller is paid on the net product amount after seller-funded discounts
        // only. Wallet points/credit and gift cards are platform payment
        // instruments; they must not reduce seller pay.
        val sellerFundedDiscount =
            order.discountAmount
                .subtract(order.walletDiscount)
                .subtract(order.giftCardDiscount)
                .max(BigDecimal.ZERO)
        val inclusiveTax =
            OrderItemDAO.itemsForOrder(order.id)
                .filter { it.taxInclusive }
                .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.taxAmount) }
        val netSubTotal =
            order.subTotal
                .subtract(sellerFundedDiscount)
                .subtract(inclusiveTax)
                .max(BigDecimal.ZERO)
        val commission = CommissionService().commissionForOrderTx(order, seller)
        val effectiveRate =
            if (netSubTotal > BigDecimal.ZERO) {
                commission
                    .multiply(BigDecimal(100))
                    .divide(netSubTotal, 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO.setScale(2)
            }
        return SellerPayoutDAO.new {
            this.sellerId = seller.id
            this.orderId = order.id
            subTotal = netSubTotal
            commissionAmount = commission
            commissionRate = effectiveRate
            payoutAmount = netSubTotal.subtract(commission)
            currency = order.currency
            status = PayoutStatus.PENDING.name
        }
    }
}
