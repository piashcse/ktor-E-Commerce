package com.piashcse.model.response

import com.piashcse.constants.OrderStatus
import kotlinx.serialization.Serializable

@Serializable
data class OrderResponse(
    val orderId: String,
    val subTotal: Float,
    val total: Float,
    val status: OrderStatus,
)