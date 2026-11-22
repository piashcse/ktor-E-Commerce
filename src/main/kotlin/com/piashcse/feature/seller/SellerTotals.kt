package com.piashcse.feature.seller

import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.calcCommission
import com.piashcse.utils.extension.entityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import java.math.BigDecimal

/**
 * Seller dashboard totals use the same net revenue basis as [PayoutLedger]:
 * seller-funded discounts and inclusive tax are excluded; wallet credit/gift
 * cards are platform payment instruments.
 */
object SellerTotals {
    /**
     * @param items pre-loaded order lines when the caller processes a batch;
     * otherwise one lookup is performed.
     */
    fun applyOrderTx(
        order: OrderDAO,
        items: List<OrderItemDAO>? = null,
    ) {
        val lines = items ?: OrderItemDAO.itemsForOrder(order.id)
        applyOrdersTx(listOf(order), mapOf(order.id.value to lines))
    }

    /** Batch variant used by split-shop checkout: one seller lookup for all orders. */
    fun applyOrdersTx(
        orders: List<OrderDAO>,
        itemsByOrder: Map<String, List<OrderItemDAO>>,
    ) {
        val shopIds = orders.mapNotNull { it.shopId?.value }.distinct()
        if (shopIds.isEmpty()) return
        val sellersByShop =
            SellerDAO.find {
                SellerTable.shopId inList shopIds.map { it.entityID(ShopTable) }
            }.associateBy { it.shopId?.value }
        orders.forEach { order ->
            val seller = sellersByShop[order.shopId?.value] ?: return@forEach
            val netSales = netSales(order, itemsByOrder[order.id.value].orEmpty())
            seller.totalSales = seller.totalSales.add(netSales)
            seller.totalCommission = seller.totalCommission.add(seller.calcCommission(netSales))
        }
    }

    fun netSales(
        order: OrderDAO,
        items: List<OrderItemDAO>,
    ): BigDecimal {
        val sellerFundedDiscount =
            order.discountAmount
                .subtract(order.walletDiscount)
                .subtract(order.giftCardDiscount)
                .max(BigDecimal.ZERO)
        val inclusiveTax =
            items.filter { it.taxInclusive }.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.taxAmount) }
        return order.subTotal
            .subtract(sellerFundedDiscount)
            .subtract(inclusiveTax)
            .max(BigDecimal.ZERO)
    }

    /** Reverses the same net basis applied by [applyOrderTx] (cancellation/full refund). */
    fun reverseOrderTx(
        order: OrderDAO,
        items: List<OrderItemDAO>? = null,
    ) {
        val lines = items ?: OrderItemDAO.itemsForOrder(order.id)
        val seller = SellerDAO.find { SellerTable.shopId eq order.shopId }.firstOrNull() ?: return
        val net = netSales(order, lines)
        seller.totalSales = seller.totalSales.subtract(net).max(BigDecimal.ZERO)
        seller.totalCommission = seller.totalCommission.subtract(seller.calcCommission(net)).max(BigDecimal.ZERO)
    }
}
