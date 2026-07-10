package com.piashcse.mapper

import com.piashcse.database.entities.RefundRequestDAO
import java.time.format.DateTimeFormatter

fun RefundRequestDAO.toRefundRequestResponse() = com.piashcse.model.response.RefundRequestResponse(
    id = id.value,
    orderItemId = orderItemId.value,
    orderId = orderId.value,
    userId = userId.value,
    reason = reason,
    images = images,
    status = status,
    refundAmount = refundAmount?.toPlainString(),
    refundMethod = refundMethod,
    trackingNumber = trackingNumber,
    requestedAt = requestedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    resolvedAt = resolvedAt?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    createdAt = createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    updatedAt = updatedAt?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: "",
)
