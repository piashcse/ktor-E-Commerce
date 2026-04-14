package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class RefundRequestResponse(
    val id: String,
    val orderItemId: String,
    val orderId: String,
    val userId: String,
    val reason: String,
    val images: String?,
    val status: String,
    val refundAmount: Double?,
    val refundMethod: String?,
    val trackingNumber: String?,
    val requestedAt: String,
    val resolvedAt: String?,
    val createdAt: String,
    val updatedAt: String
)
