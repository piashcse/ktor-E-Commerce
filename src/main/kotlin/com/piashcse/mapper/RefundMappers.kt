package com.piashcse.mapper

import com.piashcse.database.entities.RefundRequestDAO
import com.piashcse.model.response.RefundRequestResponse
import com.piashcse.utils.common.Money
import java.time.format.DateTimeFormatter

fun RefundRequestDAO.toRefundRequestResponse() =
    RefundRequestResponse(
        id = id.value,
        orderItemId = orderItemId.value,
        orderId = orderId.value,
        userId = userId.value,
        reason = reason,
        images = images,
        status = status,
        refundAmount = refundAmount?.let { Money.str(it) },
        refundMethod = refundMethod,
        trackingNumber = trackingNumber,
        requestedAt = requestedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        resolvedAt = resolvedAt?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        createdAt = createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        updatedAt = updatedAt?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: "",
    )
