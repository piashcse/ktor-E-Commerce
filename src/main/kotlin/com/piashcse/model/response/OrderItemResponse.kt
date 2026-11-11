package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class OrderItemResponse(
    val id: String,
    val productId: String,
    val productName: String?,
    val quantity: Int,
    val price: String,
    val discountAmount: String = "0.00",
    val taxAmount: String = "0.00",
    val total: String,
    val sku: String?,
    val variantId: String? = null,
    val variantName: String? = null,
    val taxRate: String = "0.0000",
    val taxInclusive: Boolean = false,
    val taxClassId: String? = null,
    val isDigital: Boolean = false,
)
