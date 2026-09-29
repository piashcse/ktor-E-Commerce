package com.piashcse.mapper

import com.piashcse.database.entities.PaymentDAO
import com.piashcse.model.response.PaymentResponse
import com.piashcse.utils.common.Money

fun PaymentDAO.toPaymentResponse() =
    PaymentResponse(
        id = paymentId.value,
        orderId = orderId.value,
        amount = Money.str(amount),
        status = status,
        paymentMethod = paymentMethod,
        transactionId = transactionId,
    )
