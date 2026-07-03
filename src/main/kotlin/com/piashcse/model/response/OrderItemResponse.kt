package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class OrderItemResponse(
    val productId: String,
    val productName: String?,
    val quantity: Int,
    val price: String,
    val discountAmount: String = "0.00",
    val taxAmount: String = "0.00",
    val total: String,
    val sku: String?,
)
