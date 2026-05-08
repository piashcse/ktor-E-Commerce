package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class CheckoutSummaryResponse(
    val subTotal: Float,
    val shippingCost: Float,
    val taxAmount: Float = 0f,
    val discountAmount: Float = 0f,
    val total: Float,
    val itemCount: Int,
)
