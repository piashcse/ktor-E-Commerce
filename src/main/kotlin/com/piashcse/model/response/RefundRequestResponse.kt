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
    val refundAmount: String?,
    val refundMethod: RefundMethod?,
    val trackingNumber: String?,
    val requestedAt: String,
    val resolvedAt: String?,
    val createdAt: String,
    val updatedAt: String,
)
