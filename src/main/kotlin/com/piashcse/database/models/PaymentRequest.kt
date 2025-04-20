package com.piashcse.database.models

import com.piashcse.database.entities.PaymentTable
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class PaymentRequest(
    val orderId: String,
    val amount: Long,
    val status: PaymentTable.PaymentStatus,
    val paymentMethod: String,
    val transactionId: String?,
) {
    fun validation() {
        validate(this) {
            validate(PaymentRequest::orderId).isNotNull().isNotEmpty()
            validate(PaymentRequest::amount).isNotNull().isGreaterThan(0)
            validate(PaymentRequest::status).isNotNull()
            validate(PaymentRequest::paymentMethod).isNotNull()
        }
    }
}
