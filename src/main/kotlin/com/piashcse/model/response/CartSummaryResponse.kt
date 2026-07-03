package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class CartSummaryResponse(
    val items: List<CartItemSummary>,
    val subtotal: String,
    val estimatedTax: String,
    val itemCount: Int,
)

@Serializable
data class CartItemSummary(
    val productId: String,
    val productName: String,
    val price: String,
    val quantity: Int,
    val image: String?,
    val stockQuantity: Int,
    val shopId: String?,
    val shopName: String?,
)
