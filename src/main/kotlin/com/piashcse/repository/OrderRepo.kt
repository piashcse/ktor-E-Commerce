package com.piashcse.repository

import com.piashcse.entities.Order
import com.piashcse.models.order.AddOrder
import com.piashcse.utils.extension.OrderStatus

interface OrderRepo {
    suspend fun addOrder(userId: String, addOrder: AddOrder) : Order
    suspend fun getOrders(userId: String, limit: Int):List<Order>
    suspend fun updateOrder(userId: String, orderId: String, orderStatus: OrderStatus): Order
}