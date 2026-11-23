package com.piashcse.feature.seller

import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.SellerDAO
import java.math.BigDecimal

/**
 * Single entry point for seller ledger mutations: sale totals, reversals,
 * partial-refund clawbacks and payout capture.
 */
object SellerLedger {
    fun applyOrder(order: OrderDAO) = SellerTotals.applyOrderTx(order)

    fun applyOrders(
        orders: List<OrderDAO>,
        itemsByOrder: Map<String, List<OrderItemDAO>>,
    ) = SellerTotals.applyOrdersTx(orders, itemsByOrder)

    fun reverseOrder(order: OrderDAO) = SellerTotals.reverseOrderTx(order)

    fun reversePartial(
        seller: SellerDAO,
        salesAmount: BigDecimal,
        commissionAmount: BigDecimal,
    ) = SellerTotals.reversePartialTx(seller, salesAmount, commissionAmount)

    fun createPayout(order: OrderDAO) = PayoutLedger.createForOrderTx(order)
}
