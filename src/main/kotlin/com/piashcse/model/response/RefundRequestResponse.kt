package com.piashcse.model.response

import com.piashcse.constants.RefundMethod
import com.piashcse.constants.RefundStatus
import kotlinx.serialization.Serializable

@Serializable
data class RefundRequestResponse(
    val id: String,
    val orderItemId: String,
    val orderId: String,
    val userId: String,
    val reason: String,
    val images: String?,
    val status: RefundStatus,
    val currency: String = "USD",
    val quantity: Int = 1,
    val refundAmount: String?,
    val shippingRefund: String = "0.00",
    val taxRefund: String = "0.00",
    val refundMethod: RefundMethod?,
    val trackingNumber: String?,
    val requestedAt: String,
    val resolvedAt: String?,
    val createdAt: String,
    val updatedAt: String?,
    val providerRefundId: String? = null,
    val providerRefundStatus: String? = null,
)
