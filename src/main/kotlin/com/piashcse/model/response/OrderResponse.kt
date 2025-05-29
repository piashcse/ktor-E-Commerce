package com.piashcse.model.response

import com.piashcse.constants.OrderStatus

data class Order(
    val orderId: String,
    val subTotal: Float,
    val total: Float,
    val status: OrderStatus,
)