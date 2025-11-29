package com.piashcse.feature.order

import com.piashcse.constants.OrderStatus
import com.piashcse.database.entities.*
import com.piashcse.model.request.OrderRequest
import com.piashcse.model.response.Order
import com.piashcse.utils.ValidationException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq

/**
 * Controller for managing order-related operations.
 */
class OrderService : OrderRepository {

    /**
     * Creates a new order for a user and stores the associated order items.
     *
     * @param userId The ID of the user placing the order.
     * @param orderRequest The details of the order including quantity, shipping charge, subtotal, total, and items.
     * @return The created order entity with associated order items.
     * @throws Exception if there is an issue during order creation or the product is no longer in the cart.
     */
    override suspend fun createOrder(userId: String, orderRequest: OrderRequest): Order = query {
        validateOrderRequest(userId, orderRequest)

        val order = OrderDAO.new {
            this.userId = EntityID(userId, OrderTable)
            this.subTotal = java.math.BigDecimal.valueOf(orderRequest.subTotal.toDouble())
            this.total = java.math.BigDecimal.valueOf(orderRequest.total.toDouble())
        }

        orderRequest.orderItems.forEach { orderItem ->
            // Validate product exists
            val product = ProductDAO.findById(orderItem.productId) ?:
                throw ValidationException("Product with ID ${orderItem.productId} not found")

            OrderItemDAO.new {
                orderId = EntityID(order.id.value, OrderItemTable)
                productId = EntityID(orderItem.productId, OrderItemTable)
                quantity = orderItem.quantity
            }
        }

        // Clear items from cart
        orderRequest.orderItems.forEach { orderItem ->
            val cartItem = CartItemDAO.find {
                CartItemTable.userId eq userId and (CartItemTable.productId eq orderItem.productId)
            }.singleOrNull()
            cartItem?.delete()
        }

        order.response()
    }

    private fun validateOrderRequest(userId: String, request: OrderRequest) {
        if (userId.isBlank()) throw ValidationException("User ID cannot be blank")
        if (request.orderItems.isEmpty()) throw ValidationException("Order must contain at least one item")
        if (request.subTotal < 0) throw ValidationException("Subtotal cannot be negative")
        if (request.total < 0) throw ValidationException("Total cannot be negative")
        request.orderItems.forEach { item ->
            if (item.quantity <= 0) throw ValidationException("Order item quantity must be greater than 0")
        }
    }

    /**
     * Retrieves a list of orders for a user with a specified limit.
     *
     * @param userId The ID of the user for whom to retrieve orders.
     * @param limit The maximum number of orders to retrieve.
     * @return A list of order entities for the user.
     */
    override suspend fun getOrders(userId: String, limit: Int): List<Order> = query {
        OrderDAO.find { OrderTable.userId eq userId }.limit(limit).map {
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
    override suspend fun updateOrderStatus(userId: String, orderId: String, status: OrderStatus): Order = query {
        if (userId.isBlank()) throw ValidationException("User ID cannot be blank")
        if (orderId.isBlank()) throw ValidationException("Order ID cannot be blank")

        val order = OrderDAO.find {
            OrderTable.userId eq userId and (OrderTable.id eq orderId)
        }.singleOrNull() ?: throw userId.notFoundException()

        order.status = status
        order.response()
    }
}