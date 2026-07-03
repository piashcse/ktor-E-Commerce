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
)
