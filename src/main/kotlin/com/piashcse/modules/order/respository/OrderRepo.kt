package com.piashcse.modules.order.respository

import com.piashcse.database.entities.Order
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.models.order.OrderRequest

interface OrderRepo{
    /**
     * Creates a new order for a user.
     *
     * @param userId The unique identifier of the user placing the order.
     * @param request The order details.
     * @return The created order.
     */
    suspend fun createOrder(userId: String, orderRequest: OrderRequest): Order

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
    suspend fun updateOrderStatus(userId: String, orderId: String, status: OrderTable.OrderStatus): Order
}