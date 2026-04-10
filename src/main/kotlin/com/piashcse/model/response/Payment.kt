package com.piashcse.model.response

import com.piashcse.constants.PaymentStatus
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Payment(
    val id: String,
    val orderId: String,
    val amount: Double,
    val status: PaymentStatus,
    val paymentMethod: String,
    val transactionId: String?,
    val currency: String = "USD",
    @Contextual val paidAt: LocalDateTime? = null,
    val refundAmount: Double? = null
)