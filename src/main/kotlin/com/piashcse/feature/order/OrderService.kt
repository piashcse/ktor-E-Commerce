package com.piashcse.feature.order

import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.UserType
import com.piashcse.event.EventBus
import com.piashcse.event.OrderPlacedEvent
import com.piashcse.model.request.CancelOrderRequest
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.response.CheckoutSummaryResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.suspendRetryQuery
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.UnauthorizedException
import com.piashcse.utils.validator.ValidationException
import java.time.Instant

class OrderService(private val orderRepo: OrderRepository) {

    /**
     * Places an order from the user's cart and publishes after-commit events.
     * Runs in a retryable transaction: the nested repository write joins this
     * transaction, so the whole orchestration commits atomically.
     */
    suspend fun placeOrder(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): List<OrderResponse> = suspendRetryQuery {
        val access = orderRepo.getPlaceOrderAccess(userId, checkoutRequest.shippingAddressId)
        if (!access.isShippingAddressOwner)
            throw ForbiddenException(Message.Orders.SHIPPING_ADDRESS_UNAUTHORIZED)
        val orders = orderRepo.placeOrder(userId, checkoutRequest)
        val email = orderRepo.getUserEmail(userId)
        orders.forEach { order ->
            EventBus.publish(
                OrderPlacedEvent(
                    orderId = order.orderId,
                    userId = userId,
                    email = email.orEmpty(),
                    shopId = order.shopId,
                    orderNumber = order.orderNumber,
                    total = order.total.toBigDecimal(),
                ),
            )
        }
        orders
    }

    /**
     * Calculates the checkout summary. Read-only projection; business rules are
     * enforced by the repository as part of the read-model calculation.
     */
    suspend fun getCheckoutSummary(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): CheckoutSummaryResponse = orderRepo.getCheckoutSummary(userId, checkoutRequest)

    /**
     * Updates an order status, enforcing role-based status rules:
     * sellers may set CONFIRMED/DELIVERED, customers may set CANCELED/RECEIVED,
     * admins may set any status. Only order participants may perform updates.
     * Runs in a retryable transaction: authorization and transition commit
     * atomically.
     */
    suspend fun updateOrderStatus(
        userId: String,
        orderId: String,
        status: OrderStatus,
        userType: UserType,
    ): OrderResponse = suspendRetryQuery {
        val access = orderRepo.getOrderAccess(userId, orderId)
        if (!access.isCustomer && !access.isSeller && !access.isAdmin)
            throw ForbiddenException(Message.Orders.UNAUTHORIZED)

        val isAdmin = userType.isAdminOrHigher
        val statusAllowed =
            isAdmin ||
                (status in listOf(OrderStatus.CONFIRMED, OrderStatus.DELIVERED) && userType == UserType.SELLER) ||
                (status in listOf(OrderStatus.CANCELED, OrderStatus.RECEIVED) && userType == UserType.CUSTOMER)
        if (!statusAllowed) throw UnauthorizedException(Message.Orders.STATUS_NOT_ALLOWED)

        if (!OrderStatus.canTransitionTo(access.currentStatus, status))
            throw ValidationException(Message.Orders.INVALID_STATUS)

        orderRepo.applyStatusTransition(orderId, status, changedBy = userId)
    }

    /**
     * Cancels an order and restores stock. Only order participants may cancel,
     * and only orders that are not yet finalized can be canceled. Runs in a
     * retryable transaction: authorization and cancellation commit atomically.
     */
    suspend fun cancelOrder(
        orderId: String,
        userId: String,
        request: CancelOrderRequest,
        userType: UserType,
    ): OrderResponse = suspendRetryQuery {
        if (request.reason.isBlank()) throw ValidationException(Message.Orders.CANCEL_REASON_REQUIRED)

        val access = orderRepo.getOrderAccess(userId, orderId)
        if (!access.isCustomer && !access.isSeller && !access.isAdmin)
            throw ForbiddenException(Message.Orders.UNAUTHORIZED)
        if (!OrderStatus.canBeCanceled(access.currentStatus))
            throw ValidationException(Message.Orders.CANNOT_CANCEL)

        orderRepo.cancelOrder(orderId, request.reason, changedBy = userId)
    }

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
    ): PaginatedResponse<OrderResponse> =
        orderRepo.getSellerOrders(userId, limit, offset, status?.parseEnum<OrderStatus>("status"))

    suspend fun getAdminOrders(
        limit: Int,
        offset: Int,
        status: String?,
        startDate: Instant?,
        endDate: Instant?,
    ): PaginatedResponse<OrderResponse> =
        orderRepo.getAdminOrders(limit, offset, status?.parseEnum<OrderStatus>("status"), startDate, endDate)
}
