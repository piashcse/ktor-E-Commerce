package com.piashcse.model.response

import com.piashcse.constants.PaymentStatus
import kotlinx.serialization.Serializable

@Serializable
data class Payment(
    val id: String,
    val orderId: String,
    val amount: Long,
    val status: PaymentStatus,
    val paymentMethod: String,
    val transactionId: String?
)