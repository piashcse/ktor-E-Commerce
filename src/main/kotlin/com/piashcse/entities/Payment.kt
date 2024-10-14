package com.piashcse.entities

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object PaymentTable : BaseIntIdTable("payment") {
    val orderId = reference("order_id", OrderTable.id)
    val amount = long("amount")
    val status = varchar("status", 50) // e.g., "PENDING", "COMPLETED"
    val paymentMethod = varchar("payment_method", 50) // e.g., "CREDIT_CARD", "PAYPAL"
}

class PaymentEntity(id: EntityID<String>) : BaseIntEntity(id, PaymentTable) {
    companion object : BaseIntEntityClass<PaymentEntity>(PaymentTable) // Reference the Payments table
    var paymentId by PaymentTable.id
    var orderId by PaymentTable.orderId
    var amount by PaymentTable.amount
    var status by PaymentTable.status
    var paymentMethod by PaymentTable.paymentMethod
    fun response() = Payment(
        id = paymentId.value,
        orderId = orderId.value,
        amount = amount,
        status = status,
        paymentMethod = paymentMethod,
    )
}

data class Payment(
    val id: String,
    val orderId: String,
    val amount: Long,
    val status: String,
    val paymentMethod: String
)

