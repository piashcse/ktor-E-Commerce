package com.piashcse.feature.order

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.PayoutStatus
import com.piashcse.database.entities.CouponDAO
import com.piashcse.database.entities.CouponUsageDAO
import com.piashcse.database.entities.CouponUsageTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.OrderItemTable
import com.piashcse.database.entities.OrderStatusHistoryDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.PaymentDAO
import com.piashcse.database.entities.PaymentTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.ReservationStatus
import com.piashcse.database.entities.SellerPayoutDAO
import com.piashcse.database.entities.SellerPayoutTable
import com.piashcse.database.entities.StockReservationDAO
import com.piashcse.database.entities.StockReservationTable
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.database.entities.removeSales
import com.piashcse.database.entities.restoreStockFor
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.promotion.PromotionService
import com.piashcse.feature.seller.SellerTotals
import com.piashcse.feature.subscription.DigitalDeliveryService
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import java.math.BigDecimal

/**
 * Shared cancellation routine used by the customer/seller/admin cancel paths, the
 * stock-reservation cleanup job and refunds. Must be called inside a DB transaction.
 *
 * Keeps stock, sales counters, seller totals, coupon usage, reservations, payout
 * rows and the status history consistent across every cancellation entry point.
 */
object OrderCancellation {
    fun cancel(
        order: OrderDAO,
        notes: String?,
        changedBy: String?,
    ) {
        val captured =
            PaymentDAO.find {
                (PaymentTable.orderId eq order.id) and (PaymentTable.status eq PaymentStatus.COMPLETED)
            }.fold(BigDecimal.ZERO) { acc, payment -> acc.add(payment.amount) }
        if (captured > BigDecimal.ZERO) {
            throw ValidationException("Orders with captured payments must be refunded, not canceled")
        }
        // Void any un-captured offline intent so it cannot be confirmed later
        // against a canceled order.
        PaymentDAO.find {
            (PaymentTable.orderId eq order.id) and (PaymentTable.status eq PaymentStatus.PENDING)
        }.forEach {
            it.status = PaymentStatus.FAILED
            it.providerStatus = "order-canceled"
        }
        order.status = OrderStatus.CANCELED
        order.canceledDate = currentUtc()
        if (!notes.isNullOrBlank()) {
            order.notes = listOfNotNull(order.notes?.takeIf { it.isNotBlank() }, notes).joinToString("\n")
        }

        OrderStatusHistoryDAO.new {
            this.orderId = order.id
            this.status = OrderStatus.CANCELED
            this.notes = notes
            this.changedBy = changedBy?.let { it.entityID(UserTable) }
        }

        val orderItems = OrderItemDAO.find { OrderItemTable.orderId eq order.id }.toList()
        val productIds = orderItems.map { it.productId.value }
        val products =
            if (productIds.isNotEmpty()) {
                ProductDAO.find { ProductTable.id inList productIds }.associateBy { it.id.value }
            } else {
                emptyMap()
            }
        val reservations =
            StockReservationDAO.find { StockReservationTable.orderId eq order.id }.toList()
        if (reservations.isNotEmpty()) {
            reservations
                .groupBy { Triple(it.productId.value, it.variantId?.value, it.warehouseId?.value) }
                .forEach { (allocation, rows) ->
                    val product = products[allocation.first] ?: return@forEach
                    val remaining =
                        rows.sumOf { (it.quantity - it.restoredQuantity).coerceAtLeast(0) }
                    if (remaining > 0) {
                        product.restoreStockFor(allocation.second, remaining, allocation.third)
                    }
                    rows.forEach { it.restoredQuantity = it.quantity }
                }
        } else {
            // Legacy rows created before warehouse allocations were recorded.
            orderItems.forEach { orderItem ->
                products[orderItem.productId.value]
                    ?.takeUnless { it.isDigital }
                    ?.restoreStockFor(
                        orderItem.variantId?.value,
                        orderItem.quantity,
                        order.pickupWarehouseId?.value,
                    )
            }
        }
        orderItems.forEach { orderItem ->
            products[orderItem.productId.value]?.removeSales(orderItem.quantity)
        }

        reservations.forEach { it.status = ReservationStatus.RELEASED }

        reverseCouponUsage(order)
        PromotionService().reverseForOrderTx(order.id.value)
        reverseSellerTotals(order)
        reverseSellerPayout(order)
        LoyaltyService().reverseOrderTx(order.userId.value, order.id.value)
        DigitalDeliveryService().revokeOrderTx(order.id.value)
    }

    private fun reverseCouponUsage(order: OrderDAO) = reverseCouponUsageForOrder(order.id.value)

    /** Releases coupon redemption rows for an order (used by cancellation and full refund). */
    fun reverseCouponUsageForOrder(orderId: String) {
        CouponUsageDAO.find {
            CouponUsageTable.orderId eq orderId.entityID(OrderTable)
        }.forEach { usage ->
            CouponDAO.findById(usage.couponId.value)?.let { coupon ->
                coupon.usageCount = (coupon.usageCount - 1).coerceAtLeast(0)
            }
            usage.delete()
        }
    }

    private fun reverseSellerTotals(order: OrderDAO) = SellerTotals.reverseOrderTx(order)

    private fun reverseSellerPayout(order: OrderDAO) {
        SellerPayoutDAO.find { SellerPayoutTable.orderId eq order.id }.forEach { payout ->
            if (payout.status != PayoutStatus.PAID.name) {
                payout.status = PayoutStatus.REFUNDED.name
            }
        }
    }
}
