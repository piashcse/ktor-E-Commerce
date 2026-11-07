package com.piashcse.database.entities

import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object PaymentTable : BaseIdTable("payment") {
    val orderId = reference("order_id", OrderTable.id)
    val userId = reference("user_id", UserTable.id)
    val amount = decimal("amount", 10, 2)
    val currency = varchar("currency", 3).default("USD")
    val status = enumerationByName("status", 30, PaymentStatus::class).clientDefault { PaymentStatus.PENDING }
    val paymentMethod = enumerationByName("payment_method", 50, PaymentMethod::class)
    val transactionId = varchar("transaction_id", 100).nullable().uniqueIndex()

    /** Client-supplied idempotency key, scoped per user by the DB partial index. */
    val idempotencyKey = varchar("idempotency_key", 100).nullable()
}

class PaymentDAO(id: EntityID<String>) : BaseEntity(id, PaymentTable) {
    companion object : BaseEntityClass<PaymentDAO>(PaymentTable, PaymentDAO::class.java)

    var paymentId by PaymentTable.id
    var orderId by PaymentTable.orderId
    var userId by PaymentTable.userId
    var amount by PaymentTable.amount
    var currency by PaymentTable.currency
    var status by PaymentTable.status
    var paymentMethod by PaymentTable.paymentMethod
    var transactionId by PaymentTable.transactionId
    var idempotencyKey by PaymentTable.idempotencyKey
}
