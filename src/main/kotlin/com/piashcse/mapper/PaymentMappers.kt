package com.piashcse.mapper

import com.piashcse.database.entities.PaymentDAO
import com.piashcse.model.response.PaymentResponse

fun PaymentDAO.toPaymentResponse() = PaymentResponse(
    id = paymentId.value,
    orderId = orderId.value,
    amount = amount,
    status = status,
    paymentMethod = paymentMethod,
    transactionId = transactionId,
)
