package com.piashcse.model.response

import com.piashcse.constants.AppConstants
import com.piashcse.constants.RefundMethod
import com.piashcse.constants.RefundStatus
import kotlinx.serialization.Serializable

@Serializable
data class RefundRequestResponse(
    val id: String,
    val orderItemId: String,
    val orderId: String,
    val orderNumber: String? = null,
    val shopId: String? = null,
    val customerEmail: String? = null,
    val productName: String? = null,
    val userId: String,
    val reason: String,
    val images: String?,
    val decisionReason: String? = null,
    val status: RefundStatus,
    val currency: String = AppConstants.DEFAULT_CURRENCY,
    val quantity: Int = 1,
    val refundAmount: String?,
    val shippingRefund: String = "0.00",
    val taxRefund: String = "0.00",
    val refundMethod: RefundMethod?,
    val trackingNumber: String?,
    val allowedActions: List<String> = emptyList(),
    val requestedAt: String,
    val resolvedAt: String?,
    val createdAt: String,
    val updatedAt: String?,
    val providerRefundId: String? = null,
    val providerRefundStatus: String? = null,
)
