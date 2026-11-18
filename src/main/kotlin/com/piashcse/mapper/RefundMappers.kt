package com.piashcse.mapper

import com.piashcse.constants.RefundStatus
import com.piashcse.database.entities.RefundRequestDAO
import com.piashcse.model.response.RefundRequestResponse
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money

fun RefundRequestDAO.toRefundRequestResponse(
    orderNumber: String? = null,
    shopId: String? = null,
    customerEmail: String? = null,
    productName: String? = null,
) = RefundRequestResponse(
    id = id.value,
    orderItemId = orderItemId.value,
    orderId = orderId.value,
    orderNumber = orderNumber,
    shopId = shopId,
    customerEmail = customerEmail,
    productName = productName,
    userId = userId.value,
    reason = reason,
    images = images,
    decisionReason = decisionReason,
    status = status,
    currency = currency,
    quantity = quantity,
    refundAmount = refundAmount?.let { Money.str(it) },
    shippingRefund = Money.str(shippingRefund),
    taxRefund = Money.str(taxRefund),
    refundMethod = refundMethod,
    trackingNumber = trackingNumber,
    allowedActions =
        RefundStatus.entries
            .filter { RefundStatus.canTransitionTo(status, it) }
            .map { it.name },
    requestedAt = requestedAt.toUtcString(),
    resolvedAt = resolvedAt?.toUtcString(),
    createdAt = createdAt.toUtcString(),
    updatedAt = updatedAt?.toUtcString(),
    providerRefundId = providerRefundId,
    providerRefundStatus = providerRefundStatus,
)
