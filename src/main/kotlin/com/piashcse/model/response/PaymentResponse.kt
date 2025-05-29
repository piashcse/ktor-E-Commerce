package com.piashcse.model.response

import com.piashcse.constants.PaymentStatus

data class Payment(
    val id: String,
    val orderId: String,
    val amount: Long,
    val status: PaymentStatus,
    val paymentMethod: String,
    val transactionId: String?
)