package com.piashcse.model.response

import kotlinx.serialization.Serializable
import com.piashcse.constants.OrderStatus

@Serializable
data class Order(
    val orderId: String,
    val subTotal: Float,
    val total: Float,
    val status: OrderStatus,
)