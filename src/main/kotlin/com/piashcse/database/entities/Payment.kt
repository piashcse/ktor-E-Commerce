package com.piashcse.database.entities

import com.piashcse.constants.PaymentStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.model.response.Payment
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object PaymentTable : BaseIdTable("payment") {
    val orderId = reference("order_id", OrderTable.id)
    val userId = reference("user_id", UserTable.id)
    val amount = long("amount")
    val status = enumerationByName("status", 30, PaymentStatus::class).clientDefault { PaymentStatus.PENDING }
    val paymentMethod = varchar("payment_method", 50) // e.g., "CREDIT_CARD", "PAYPAL"
    val transactionId = varchar("transaction_id", 100).nullable()
}

class PaymentDAO(id: EntityID<String>) : BaseEntity(id, PaymentTable) {
    companion object : BaseEntityClass<PaymentDAO>(PaymentTable,PaymentDAO::class.java ) // Reference the Payments table

    var paymentId by PaymentTable.id
    var orderId by PaymentTable.orderId
    var amount by PaymentTable.amount
    var status by PaymentTable.status
    var paymentMethod by PaymentTable.paymentMethod
    var transactionId by PaymentTable.transactionId
    fun response() = Payment(
        id = paymentId.value,
        orderId = orderId.value,
        amount = amount,
        status = status,
        paymentMethod = paymentMethod,
        transactionId = transactionId
    )
}

