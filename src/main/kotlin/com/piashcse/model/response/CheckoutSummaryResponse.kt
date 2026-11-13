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
    val locale: String = "en",
    val formattedSubTotal: String? = null,
    val formattedShipping: String? = null,
    val formattedTax: String? = null,
    val formattedDiscount: String? = null,
    val formattedTotal: String? = null,
    /** True when taxAmount is already included in subTotal rather than added on top. */
    val taxInclusive: Boolean = false,
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
