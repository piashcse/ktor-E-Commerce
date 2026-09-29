package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object SellerPayoutTable : BaseIdTable("seller_payout") {
    val sellerId = reference("seller_id", SellerTable.id).index()
    val orderId = reference("order_id", OrderTable.id).index()
    val subTotal = decimal("sub_total", 10, 2)
    val commissionAmount = decimal("commission_amount", 10, 2)
    val payoutAmount = decimal("payout_amount", 10, 2)
    val status = varchar("status", 20).default("PENDING").index()
    val paidAt = datetime("paid_at").nullable()
}

class SellerPayoutDAO(id: EntityID<String>) : BaseEntity(id, SellerPayoutTable) {
    companion object : BaseEntityClass<SellerPayoutDAO>(SellerPayoutTable, SellerPayoutDAO::class.java)

    var sellerId by SellerPayoutTable.sellerId
    var orderId by SellerPayoutTable.orderId
    var subTotal by SellerPayoutTable.subTotal
    var commissionAmount by SellerPayoutTable.commissionAmount
    var payoutAmount by SellerPayoutTable.payoutAmount
    var status by SellerPayoutTable.status
    var paidAt by SellerPayoutTable.paidAt
}
