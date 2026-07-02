package com.piashcse.model.response

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.time.LocalDateTime

@Serializable
data class OrderResponse(
    val orderId: String,
    val orderNumber: String,
    val userId: String? = null,
    val shopId: String? = null,
    val subTotal: Float,
    val shippingCost: Float = 0f,
    val taxAmount: Float = 0f,
    val discountAmount: Float = 0f,
    val total: Float,
    val currency: String = "USD",
    val status: OrderStatus,
    val paymentStatus: PaymentStatus? = null,
    val couponCode: String? = null,
    val paymentMethod: String? = null,
    val notes: String? = null,
    val shippingAddress: String? = null,
    val billingAddress: String? = null,
    val shippingMethod: String? = null,
    val shippingDate: @Contextual LocalDateTime? = null,
    val deliveredDate: @Contextual LocalDateTime? = null,
    val canceledDate: @Contextual LocalDateTime? = null,
    val completedDate: @Contextual LocalDateTime? = null,
    val createdAt: @Contextual LocalDateTime? = null,
    val updatedAt: @Contextual LocalDateTime? = null,
    val items: List<OrderItemResponse>? = null,
)
