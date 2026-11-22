package com.piashcse.model.response

import com.piashcse.constants.AppConstants
import kotlinx.serialization.Serializable

@Serializable
data class CartSummaryResponse(
    val items: List<CartItemSummary>,
    val subtotal: String,
    val estimatedTax: String,
    val itemCount: Int,
    /** Subtotal + estimated shipping + exclusive tax. */
    val total: String = subtotal,
    val currency: String = AppConstants.DEFAULT_CURRENCY,
    /** Cheapest available shipping for the whole cart (already multiplied by shop count). */
    val estimatedShipping: String = "0.00",
    val freeShippingThreshold: String = "75.00",
    val freeShippingRemaining: String = "0.00",
    val shops: List<CartShopSummary> = emptyList(),
)

@Serializable
data class CartShopSummary(
    val shopId: String?,
    val shopName: String?,
    val subtotal: String,
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
    val variantId: String? = null,
    val variantName: String? = null,
)
