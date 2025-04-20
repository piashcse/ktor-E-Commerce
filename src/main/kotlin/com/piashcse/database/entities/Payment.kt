package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object PaymentTable : BaseIntIdTable("payment") {
    val orderId = reference("order_id", OrderTable.id)
    val userId = reference("user_id", UserTable.id)
    val amount = long("amount")
    val status = enumerationByName("status", 30, PaymentStatus::class).clientDefault { PaymentStatus.PENDING }
    val paymentMethod = varchar("payment_method", 50) // e.g., "CREDIT_CARD", "PAYPAL"
    val transactionId = varchar("transaction_id", 100).nullable()

    enum class PaymentStatus {
        PENDING, COMPLETED, FAILED, REFUNDED
    }
}

class PaymentDAO(id: EntityID<String>) : BaseIntEntity(id, PaymentTable) {
    companion object : BaseIntEntityClass<PaymentDAO>(PaymentTable) // Reference the Payments table

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

data class Payment(
    val id: String,
    val orderId: String,
    val amount: Long,
    val status: PaymentTable.PaymentStatus,
    val paymentMethod: String,
    val transactionId: String?
)

