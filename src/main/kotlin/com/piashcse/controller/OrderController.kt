package com.piashcse.controller

import com.piashcse.entities.CartItemEntity
import com.piashcse.entities.CartItemTable
import com.piashcse.entities.orders.*
import com.piashcse.models.order.AddOrder
import com.piashcse.repository.OrderRepo
import com.piashcse.utils.extension.OrderStatus
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.orderStatusCode
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

class OrderController : OrderRepo {
    override suspend fun addOrder(userId: String, addOrder: AddOrder): Order = query {
        val order = OrderEntity.new {
            this.userId = EntityID(userId, OrderTable)
            this.quantity = addOrder.quantity
            this.shippingCharge = addOrder.shippingCharge
            this.subTotal = addOrder.subTotal
            this.total = addOrder.total
        }
        addOrder.orderItems.forEach {
            OrderItemEntity.new {
                orderId = EntityID(order.id.value, OrderItemTable)
                productId = EntityID(it.productId, OrderItemTable)
                quantity = it.quantity
            }
        }
        addOrder.orderItems.forEach {
            val productExist =
                CartItemEntity.find { CartItemTable.userId eq userId and (CartItemTable.productId eq it.productId) }
                    .toList().singleOrNull()
            productExist?.delete()

        }
        order.response()
    }

    override suspend fun getOrders(userId: String, limit: Int, offset: Long): List<Order> = query {
        OrderEntity.find { OrderTable.userId eq userId }.limit(limit, offset).map {
            it.response()
        }
    }

    override suspend fun updateOrder(userId: String, orderId: String, orderStatus: OrderStatus): Order = query {
        val isOrderExist =
            OrderEntity.find { OrderTable.userId eq userId and (OrderTable.id eq orderId) }.toList().singleOrNull()
        isOrderExist?.let {
            it.status = orderStatus.name.lowercase()
            it.statusCode = orderStatus.name.lowercase().orderStatusCode()
            it.response()
        } ?: throw userId.notFoundException()
    }
}