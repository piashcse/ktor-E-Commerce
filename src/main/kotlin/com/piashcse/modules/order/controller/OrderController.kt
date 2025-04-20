package com.piashcse.modules.order.controller

import com.piashcse.database.entities.CartItemDAO
import com.piashcse.database.entities.CartItemTable
import com.piashcse.database.entities.Order
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.OrderItemTable
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.models.order.OrderRequest
import com.piashcse.modules.order.respository.OrderRepo
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

/**
 * Controller for managing order-related operations.
 */
class OrderController : OrderRepo {

    /**
     * Creates a new order for a user and stores the associated order items.
     *
     * @param userId The ID of the user placing the order.
     * @param orderRequest The details of the order including quantity, shipping charge, subtotal, total, and items.
     * @return The created order entity with associated order items.
     * @throws Exception if there is an issue during order creation or the product is no longer in the cart.
     */
    override suspend fun createOrder(userId: String, orderRequest: OrderRequest): Order = query {
        val order = OrderDAO.Companion.new {
            this.userId = EntityID(userId, OrderTable)
            this.subTotal = orderRequest.subTotal
            this.total = orderRequest.total
        }
        orderRequest.orderItems.forEach {
            OrderItemDAO.Companion.new {
                orderId = EntityID(order.id.value, OrderItemTable)
                productId = EntityID(it.productId, OrderItemTable)
                quantity = it.quantity
            }
        }
        orderRequest.orderItems.forEach {
            val productExist =
                CartItemDAO.Companion.find { CartItemTable.userId eq userId and (CartItemTable.productId eq it.productId) }
                    .toList().singleOrNull()
            productExist?.delete()
        }
        order.response()
    }

    /**
     * Retrieves a list of orders for a user with a specified limit.
     *
     * @param userId The ID of the user for whom to retrieve orders.
     * @param limit The maximum number of orders to retrieve.
     * @return A list of order entities for the user.
     */
    override suspend fun getOrders(userId: String, limit: Int): List<Order> = query {
        OrderDAO.Companion.find { OrderTable.userId eq userId }.limit(limit).map {
            it.response()
        }
    }

    /**
     * Updates the status of a user's order.
     *
     * @param userId The ID of the user whose order status is being updated.
     * @param orderId The ID of the order to be updated.
     * @param status The new status of the order.
     * @return The updated order entity with the new status.
     * @throws Exception if the order does not exist for the given user.
     */
    override suspend fun updateOrderStatus(userId: String, orderId: String, status: OrderTable.OrderStatus): Order =
        query {
            val isOrderExist =
                OrderDAO.Companion.find { OrderTable.userId eq userId and (OrderTable.id eq orderId) }.toList()
                    .singleOrNull()
            isOrderExist?.let {
                it.status = status
                it.response()
            } ?: throw userId.notFoundException()
        }
}