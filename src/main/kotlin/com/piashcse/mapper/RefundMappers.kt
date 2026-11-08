package com.piashcse.mapper

import com.piashcse.database.entities.RefundRequestDAO
import com.piashcse.model.response.RefundRequestResponse
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money

fun RefundRequestDAO.toRefundRequestResponse() =
    RefundRequestResponse(
        id = id.value,
        orderItemId = orderItemId.value,
        orderId = orderId.value,
        userId = userId.value,
        reason = reason,
        images = images,
        status = status,
        currency = currency,
        quantity = quantity,
        refundAmount = refundAmount?.let { Money.str(it) },
        shippingRefund = Money.str(shippingRefund),
        taxRefund = Money.str(taxRefund),
        refundMethod = refundMethod,
        trackingNumber = trackingNumber,
        requestedAt = requestedAt.toUtcString(),
        resolvedAt = resolvedAt?.toUtcString(),
        createdAt = createdAt.toUtcString(),
        updatedAt = updatedAt?.toUtcString(),
    )
