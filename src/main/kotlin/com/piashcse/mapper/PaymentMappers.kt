package com.piashcse.mapper

import com.piashcse.database.entities.PaymentDAO
import com.piashcse.model.response.PaymentResponse
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money

fun PaymentDAO.toPaymentResponse() =
    PaymentResponse(
        id = paymentId.value,
        orderId = orderId.value,
        amount = Money.str(amount),
        currency = currency,
        status = status,
        paymentMethod = paymentMethod,
        transactionId = transactionId,
        createdAt = createdAt.toUtcString(),
        provider = provider,
        providerReference = providerReference,
        clientSecret = clientSecret,
        nextActionUrl = nextActionUrl,
        scaStatus = scaStatus,
        providerStatus = providerStatus,
        riskScore = riskScore,
        riskDecision = riskDecision,
        avsStatus = avsStatus,
        cvvStatus = cvvStatus,
        threeDsEnforced = threeDsEnforced,
    )
