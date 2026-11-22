package com.piashcse.feature.dashboard

import com.piashcse.constants.*
import com.piashcse.database.entities.*
import com.piashcse.model.response.*
import com.piashcse.utils.common.Money
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.select
import java.math.BigDecimal

/** Shared product revenue/refund aggregation for platform and seller dashboards. */
internal object DashboardRevenue {
    fun productRevenueByProduct(
        productIds: List<EntityID<String>>,
        shopIds: List<EntityID<String>>? = null,
    ): Map<String, BigDecimal> {
        if (productIds.isEmpty()) return emptyMap()
        var condition: Op<Boolean> =
            (OrderItemTable.productId inList productIds) and
                (OrderTable.status neq OrderStatus.CANCELED) and
                (OrderTable.paymentStatus eq PaymentStatus.COMPLETED)
        if (shopIds != null) {
            condition = condition and (OrderTable.shopId inList shopIds)
        }
        val refunds = refundedByProduct(productIds, shopIds)
        return (OrderItemTable innerJoin OrderTable)
            .select(
                OrderItemTable.productId,
                OrderItemTable.total,
                OrderItemTable.discountAmount,
                OrderTable.fxRate,
            )
            .where { condition }
            .toList()
            .groupBy { it[OrderItemTable.productId].value }
            .mapValues { (productId, rows) ->
                val gross =
                    rows.fold(BigDecimal.ZERO) { acc, row ->
                        val net = row[OrderItemTable.total].subtract(row[OrderItemTable.discountAmount])
                        acc.add(Money.toUsd(net.max(BigDecimal.ZERO), row[OrderTable.fxRate]))
                    }
                gross.subtract(refunds[productId] ?: BigDecimal.ZERO).max(BigDecimal.ZERO)
            }
    }

    /** Customer refunds (item amount only) per product, converted to USD. */
    fun refundedByProduct(
        productIds: List<EntityID<String>>,
        shopIds: List<EntityID<String>>? = null,
    ): Map<String, BigDecimal> {
        if (productIds.isEmpty()) return emptyMap()
        var condition: Op<Boolean> =
            (OrderItemTable.productId inList productIds) and
                (RefundRequestTable.status eq RefundStatus.REFUNDED)
        if (shopIds != null) {
            condition = condition and (OrderTable.shopId inList shopIds)
        }
        return RefundRequestTable
            .join(OrderItemTable, JoinType.INNER, RefundRequestTable.orderItemId, OrderItemTable.id)
            .join(OrderTable, JoinType.INNER, RefundRequestTable.orderId, OrderTable.id)
            .select(OrderItemTable.productId, RefundRequestTable.refundAmount, OrderTable.fxRate)
            .where { condition }
            .toList()
            .groupBy { it[OrderItemTable.productId].value }
            .mapValues { (_, rows) ->
                rows.fold(BigDecimal.ZERO) { acc, row ->
                    val amount = row[RefundRequestTable.refundAmount] ?: BigDecimal.ZERO
                    acc.add(Money.toUsd(amount.max(BigDecimal.ZERO), row[OrderTable.fxRate]))
                }
            }
    }
}
