package com.piashcse.feature.order

import com.piashcse.model.response.OrderResponse
import com.piashcse.utils.common.PaginatedResponse
import java.time.Instant

class OrderQueryService(private val orderRepo: OrderRepository) {

    suspend fun getOrders(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<OrderResponse> =
        orderRepo.getOrders(userId, limit, offset)

    suspend fun getSellerOrders(
        userId: String,
        limit: Int,
        offset: Int,
        status: String?,
    ): PaginatedResponse<OrderResponse> =
        orderRepo.getSellerOrders(userId, limit, offset, status)

    suspend fun getAdminOrders(
        limit: Int,
        offset: Int,
        status: String?,
        startDate: Instant?,
        endDate: Instant?,
    ): PaginatedResponse<OrderResponse> =
        orderRepo.getAdminOrders(limit, offset, status, startDate, endDate)
}
