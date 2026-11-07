package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

/**
 * Aggregate for a seller payout request. Rows in [SellerPayoutTable] point at a
 * request so idempotency is scoped per seller and a request can span many ledger
 * rows without violating a per-row unique constraint.
 */
object PayoutRequestTable : BaseIdTable("payout_request") {
    val sellerId = reference("seller_id", SellerTable.id).index()
    val idempotencyKey = varchar("idempotency_key", 100).nullable()
    val amount = decimal("amount", 12, 2)
    val currency = varchar("currency", 3).default("USD")
    val status = varchar("status", 20).default("REQUESTED").index()

    init {
        index(customIndexName = "payout_request_seller_idx", isUnique = false, sellerId)
    }
}

class PayoutRequestDAO(id: EntityID<String>) : BaseEntity(id, PayoutRequestTable) {
    companion object : BaseEntityClass<PayoutRequestDAO>(PayoutRequestTable, PayoutRequestDAO::class.java)

    var sellerId by PayoutRequestTable.sellerId
    var idempotencyKey by PayoutRequestTable.idempotencyKey
    var amount by PayoutRequestTable.amount
    var currency by PayoutRequestTable.currency
    var status by PayoutRequestTable.status
}
