package com.piashcse.model.response

import com.piashcse.constants.PaymentMethod
import com.piashcse.constants.PaymentStatus
import kotlinx.serialization.Serializable

@Serializable
data class PaymentResponse(
    val id: String,
    val orderId: String,
    val amount: Long,
    val status: PaymentStatus,
    val paymentMethod: PaymentMethod,
    val transactionId: String?,
)
