package com.piashcse.feature.order

import com.piashcse.constants.OrderStatus
import com.piashcse.model.request.OrderRequest
import com.piashcse.model.response.Order

interface OrderRepository {
    /**
     * Creates a new order for a user.
     *
     * @param userId The unique identifier of the user placing the order.
     * @param request The order details.
     * @param idempotencyKey Optional key to prevent duplicate order creation.
     * @return The created order.
     */
    suspend fun createOrder(userId: String, orderRequest: OrderRequest, idempotencyKey: String? = null): List<Order>

    /**
     * Retrieves a list of orders for a user.
     *
     * @param userId The unique identifier of the user.
     * @param limit The maximum number of orders to return.
     * @return A list of orders.
     */
    suspend fun getOrders(userId: String, limit: Int): List<Order>

    /**
     * Updates the status of an order.
     *
     * @param userId The unique identifier of the user.
     * @param orderId The unique identifier of the order.
     * @param status The updated order status.
     * @return The updated order.
     */
    suspend fun updateOrderStatus(userId: String, orderId: String, status: OrderStatus): Order

    /**
     * Cancels an order and restores stock quantities.
     *
     * @param orderId The unique identifier of the order to cancel.
     * @param userId The unique identifier of the user requesting cancellation.
     * @param reason The reason for cancellation.
     * @param userType The type of user (CUSTOMER, SELLER, ADMIN, SUPER_ADMIN).
     * @return The updated order with CANCELED status.
     */
    suspend fun cancelOrder(orderId: String, userId: String, reason: String, userType: com.piashcse.constants.UserType): Order

    /**
     * Retrieves orders for a seller's shop.
     *
     * @param userId The unique identifier of the seller.
     * @param limit The maximum number of orders to return.
     * @param offset The offset for pagination.
     * @param status Optional status filter.
     * @return A list of orders for the seller's shop.
     */
    suspend fun getSellerOrders(userId: String, limit: Int, offset: Int, status: String?): List<Order>

    /**
     * Retrieves all orders with optional filters for admin.
     *
     * @param limit The maximum number of orders to return.
     * @param offset The offset for pagination.
     * @param status Optional status filter.
     * @param startDate Optional start date filter.
     * @param endDate Optional end date filter.
     * @return A pair containing the list of orders and the total count.
     */
    suspend fun getAdminOrders(
        limit: Int,
        offset: Int,
        status: String?,
        startDate: java.time.Instant?,
        endDate: java.time.Instant?
    ): Pair<List<Order>, Long>
}