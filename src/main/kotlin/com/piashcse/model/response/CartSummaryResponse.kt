package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class CartSummaryResponse(
    val items: List<CartItemSummary>,
    val subtotal: Double,
    val estimatedTax: Double,
    val itemCount: Int
)

@Serializable
data class CartItemSummary(
    val productId: String,
    val productName: String,
    val price: Double,
    val quantity: Int,
    val image: String?,
    val stockQuantity: Int,
    val shopId: String?,
    val shopName: String?
)
