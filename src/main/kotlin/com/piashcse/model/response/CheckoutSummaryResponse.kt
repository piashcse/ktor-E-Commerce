package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class CheckoutSummaryResponse(
    val subTotal: String,
    val shippingCost: String,
    val taxAmount: String = "0.00",
    val discountAmount: String = "0.00",
    val total: String,
    val itemCount: Int,
    val currency: String = "USD",
    val taxLines: List<TaxLineResponse> = emptyList(),
    val walletDiscount: String = "0.00",
    val giftCardDiscount: String = "0.00",
    val pointsBalance: Int = 0,
    val storeCreditBalance: String = "0.00",
)

@Serializable
data class TaxLineResponse(
    val productId: String,
    val taxClassId: String? = null,
    val rate: String,
    val inclusive: Boolean,
    val netAmount: String,
    val taxAmount: String,
    val grossAmount: String,
)
