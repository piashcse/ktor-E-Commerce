package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class OrderItemResponse(
    val productId: String,
    val productName: String?,
    val quantity: Int,
    val price: Float,
    val discountAmount: Float = 0f,
    val taxAmount: Float = 0f,
    val total: Float,
    val sku: String?,
)
