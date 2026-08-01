package com.piashcse.feature.order

import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.UserType
import com.piashcse.model.request.CancelOrderRequest
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.response.CheckoutSummaryResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.validator.UnauthorizedException
import java.time.Instant

class OrderService(private val orderRepo: OrderRepository) {
    suspend fun getOrders(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<OrderResponse> = orderRepo.getOrders(userId, limit, offset)

    suspend fun getSellerOrders(
        userId: String,
        limit: Int,
        offset: Int,
        status: String?,
    ): PaginatedResponse<OrderResponse> = orderRepo.getSellerOrders(userId, limit, offset, status)

    suspend fun getAdminOrders(
        limit: Int,
        offset: Int,
        status: String?,
        startDate: Instant?,
        endDate: Instant?,
    ): PaginatedResponse<OrderResponse> = orderRepo.getAdminOrders(limit, offset, status, startDate, endDate)

    /**
     * Updates an order status, enforcing the role-based status rules:
     * sellers may set CONFIRMED/DELIVERED, customers may set CANCELED/RECEIVED, admins may set any status.
     */
    suspend fun updateOrderStatus(
        userId: String,
        orderId: String,
        status: OrderStatus,
        userType: UserType,
    ): OrderResponse {
        val isAdmin = userType in listOf(UserType.ADMIN, UserType.SUPER_ADMIN)
        val statusAllowed =
            isAdmin ||
                (status in listOf(OrderStatus.CONFIRMED, OrderStatus.DELIVERED) && userType == UserType.SELLER) ||
                (status in listOf(OrderStatus.CANCELED, OrderStatus.RECEIVED) && userType == UserType.CUSTOMER)
        if (!statusAllowed) throw UnauthorizedException(Message.Orders.STATUS_NOT_ALLOWED)
        return orderRepo.updateOrderStatus(userId, orderId, status)
    }

    suspend fun cancelOrder(
        orderId: String,
        userId: String,
        request: CancelOrderRequest,
        userType: UserType,
    ): OrderResponse = orderRepo.cancelOrder(orderId, userId, request.reason, userType)

    suspend fun getCheckoutSummary(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): CheckoutSummaryResponse = orderRepo.getCheckoutSummary(userId, checkoutRequest)

    suspend fun placeOrder(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): List<OrderResponse> = orderRepo.placeOrder(userId, checkoutRequest)
}
