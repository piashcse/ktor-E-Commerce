package com.piashcse.controller

import com.piashcse.entities.orders.*
import com.piashcse.models.PagingData
import com.piashcse.models.order.AddOrder
import com.piashcse.models.order.OrderId
import com.piashcse.utils.extension.OrderStatus
import com.piashcse.utils.extension.isNotExistException
import com.piashcse.utils.extension.orderStatusCode
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

class OrderController {
    suspend fun createOrder(userId: String, addOrder: AddOrder) = query {
        val order = OrderEntity.new {
            this.userId = EntityID(userId, OrdersTable)
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
        order.orderCreatedResponse()
    }

   suspend fun getOrders(userId: String, limit: Int, offset:Long) = query {
        OrderEntity.find { OrdersTable.userId eq userId }.limit(limit, offset).map {
            it.response()
        }
    }

    suspend fun updateOrder(userId: String, orderId: String, orderStatus: OrderStatus) = query {
        val orderExist =
            OrderEntity.find { OrdersTable.userId eq userId and (OrdersTable.id eq orderId) }.toList()
                .singleOrNull()
        orderExist?.let {
            it.status = orderStatus.name.lowercase()
            it.statusCode = orderStatus.name.lowercase().orderStatusCode()
            it.response()
        }?: "".isNotExistException()
    }
}