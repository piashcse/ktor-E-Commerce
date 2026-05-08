package com.piashcse.model.response

import com.piashcse.constants.OrderStatus
import kotlinx.serialization.Serializable

@Serializable
data class OrderResponse(
    val orderId: String,
    val orderNumber: String,
    val subTotal: Float,
    val shippingCost: Float,
    val total: Float,
    val status: OrderStatus,
    val shippingAddress: String?,
    val shippingMethod: String?,
    val items: List<OrderItemResponse>? = null,
)
