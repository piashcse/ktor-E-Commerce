package com.piashcse.model.response

import kotlinx.serialization.Serializable
import com.piashcse.constants.PaymentStatus

@Serializable
data class Payment(
    val id: String,
    val orderId: String,
    val amount: Long,
    val status: PaymentStatus,
    val paymentMethod: String,
    val transactionId: String?
)