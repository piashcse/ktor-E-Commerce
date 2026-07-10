package com.piashcse.feature.order

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.UserType
import com.piashcse.model.request.OrderRequest
import com.piashcse.model.response.OrderResponse

class OrderManagementService(private val orderRepo: OrderRepository) {

    suspend fun createOrder(
        userId: String,
        orderRequest: OrderRequest,
        idempotencyKey: String? = null,
    ): List<OrderResponse> =
        orderRepo.createOrder(userId, orderRequest, idempotencyKey)

    suspend fun updateOrderStatus(
        userId: String,
        orderId: String,
        status: OrderStatus,
    ): OrderResponse =
        orderRepo.updateOrderStatus(userId, orderId, status)

    suspend fun cancelOrder(
        orderId: String,
        userId: String,
        reason: String,
        userType: UserType,
    ): OrderResponse =
        orderRepo.cancelOrder(orderId, userId, reason, userType)
}
