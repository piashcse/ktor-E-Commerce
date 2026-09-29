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

    // Payout request lifecycle (V17): there is no status enum, so the REQUESTED /
    // APPROVED states reuse PENDING plus timestamps — requestedAt set = REQUESTED,
    // approvedAt set = APPROVED, rejectedAt set = terminal REJECTED. paidAt/PAID
    // stays the terminal paid state and markPaid only transitions from PENDING.
    val requestedAt = datetime("requested_at").nullable()
    val approvedAt = datetime("approved_at").nullable()
    val rejectedAt = datetime("rejected_at").nullable()

    // Retry-safe unique guard for seller payout requests: concurrent retries with
    // the same Idempotency-Key collide on the partial unique index (V17) and the
    // second writer either replays the existing rows or surfaces a 409.
    val payoutKey = varchar("payout_key", 100).nullable()

    init {
        uniqueIndex("seller_payout_seller_order_unique", sellerId, orderId)
    }
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
    var requestedAt by SellerPayoutTable.requestedAt
    var approvedAt by SellerPayoutTable.approvedAt
    var rejectedAt by SellerPayoutTable.rejectedAt
    var payoutKey by SellerPayoutTable.payoutKey
}
