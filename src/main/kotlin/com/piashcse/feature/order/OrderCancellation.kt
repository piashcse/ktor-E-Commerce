package com.piashcse.feature.order

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.CouponDAO
import com.piashcse.database.entities.CouponUsageTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.OrderItemTable
import com.piashcse.database.entities.OrderStatusHistoryDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.ReservationStatus
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerPayoutDAO
import com.piashcse.database.entities.SellerPayoutTable
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.StockReservationDAO
import com.piashcse.database.entities.StockReservationTable
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.database.entities.calcCommission
import com.piashcse.database.entities.removeSales
import com.piashcse.database.entities.restoreStock
import com.piashcse.utils.extension.entityID
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
        order.status = OrderStatus.CANCELED
        order.canceledDate = currentUtc()
        if (!notes.isNullOrBlank()) {
            order.notes = listOfNotNull(order.notes?.takeIf { it.isNotBlank() }, notes).joinToString("\n")
        }
        if (order.paymentStatus == PaymentStatus.COMPLETED) {
            order.paymentStatus = PaymentStatus.REFUNDED
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
        orderItems.forEach { orderItem ->
            products[orderItem.productId.value]?.restoreStock(orderItem.quantity)
            products[orderItem.productId.value]?.removeSales(orderItem.quantity)
        }

        StockReservationDAO.find { StockReservationTable.orderId eq order.id }
            .forEach { it.status = ReservationStatus.RELEASED }

        reverseCouponUsage(order)
        reverseSellerTotals(order)
        reverseSellerPayout(order)
    }

    private fun reverseCouponUsage(order: OrderDAO) = reverseCouponUsageForOrder(order.id.value)

    /** Releases coupon redemption rows for an order (used by cancellation and full refund). */
    fun reverseCouponUsageForOrder(orderId: String) {
        com.piashcse.database.entities.CouponUsageDAO.find {
            CouponUsageTable.orderId eq orderId.entityID(OrderTable)
        }.forEach { usage ->
            CouponDAO.findById(usage.couponId.value)?.let { coupon ->
                coupon.usageCount = (coupon.usageCount - 1).coerceAtLeast(0)
            }
            usage.delete()
        }
    }

    private fun reverseSellerTotals(order: OrderDAO) {
        val seller =
            SellerDAO.find { SellerTable.shopId eq order.shopId }.firstOrNull() ?: return
        val subtract = order.subTotal.max(BigDecimal.ZERO)
        seller.totalSales = seller.totalSales.subtract(subtract).max(BigDecimal.ZERO)
        seller.totalCommission = seller.totalCommission.subtract(seller.calcCommission(subtract)).max(BigDecimal.ZERO)
    }

    private fun reverseSellerPayout(order: OrderDAO) {
        SellerPayoutDAO.find { SellerPayoutTable.orderId eq order.id }.forEach { payout ->
            if (payout.status != "PAID") {
                payout.status = "REFUNDED"
            }
        }
    }
}
