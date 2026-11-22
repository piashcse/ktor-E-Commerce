package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

/**
 * Seller debt created after a payout was already paid and the order was later
 * refunded/charged back. Negative amounts are recovered from future payouts.
 */
object SellerBalanceAdjustmentTable : BaseIdTable("seller_balance_adjustment") {
    val sellerId = reference("seller_id", SellerTable.id).index()
    val orderId = reference("order_id", OrderTable.id).nullable().index()
    val amount = decimal("amount", 12, 2)
    val currency = varchar("currency", 3)
    val reason = varchar("reason", 100)
    val settledAt = datetime("settled_at").nullable()
}

class SellerBalanceAdjustmentDAO(id: EntityID<String>) : BaseEntity(id, SellerBalanceAdjustmentTable) {
    companion object :
        BaseEntityClass<SellerBalanceAdjustmentDAO>(SellerBalanceAdjustmentTable, SellerBalanceAdjustmentDAO::class.java)

    var sellerId by SellerBalanceAdjustmentTable.sellerId
    var orderId by SellerBalanceAdjustmentTable.orderId
    var amount by SellerBalanceAdjustmentTable.amount
    var currency by SellerBalanceAdjustmentTable.currency
    var reason by SellerBalanceAdjustmentTable.reason
    var settledAt: LocalDateTime? by SellerBalanceAdjustmentTable.settledAt
}
