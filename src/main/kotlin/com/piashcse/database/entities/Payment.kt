package com.piashcse.database.entities

import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.model.response.Payment
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object PaymentTable : BaseIdTable("payment") {
    val orderId = reference("order_id", OrderTable.id)
    val userId = reference("user_id", UserTable.id)
    val amount = decimal("amount", 10, 2)
    val status = enumerationByName("status", 30, PaymentStatus::class).clientDefault { PaymentStatus.PENDING }
    val paymentMethod = varchar("payment_method", 50)
    val transactionId = varchar("transaction_id", 100).nullable()
    val currency = varchar("currency", 3).default("USD")
    val paidAt = datetime("paid_at").nullable()
    val refundAmount = decimal("refund_amount", 10, 2).nullable()
    val refundReason = varchar("refund_reason", 500).nullable()
}

class PaymentDAO(id: EntityID<String>) : BaseEntity(id, PaymentTable) {
    companion object : BaseEntityClass<PaymentDAO>(PaymentTable,PaymentDAO::class.java ) // Reference the Payments table

    var paymentId by PaymentTable.id
    var orderId by PaymentTable.orderId
    var userId by PaymentTable.userId
    var amount by PaymentTable.amount
    var status by PaymentTable.status
    var paymentMethod by PaymentTable.paymentMethod
    var transactionId by PaymentTable.transactionId
    var currency by PaymentTable.currency
    var paidAt by PaymentTable.paidAt
    var refundAmount by PaymentTable.refundAmount
    var refundReason by PaymentTable.refundReason

    fun response() = Payment(
        id = paymentId.value,
        orderId = orderId.value,
        amount = amount.toDouble(),
        status = status,
        paymentMethod = paymentMethod,
        transactionId = transactionId,
        currency = currency,
        paidAt = paidAt,
        refundAmount = refundAmount?.toDouble()
    )
}

