package com.piashcse.feature.order

import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.mapper.toOrderItemResponse
import com.piashcse.model.response.OrderItemResponse

/** Shared order-line mapping used by order queries and lifecycle responses. */
internal fun List<OrderItemDAO>.toItemResponses(): List<OrderItemResponse> = map { it.toOrderItemResponse() }

/** Batch-loads order lines so list endpoints never issue one query per order. */
internal fun loadItemsForOrders(orders: List<OrderDAO>): Map<String, List<OrderItemResponse>> =
    OrderItemDAO.itemsForOrders(orders.map { it.id })
        .mapValues { (_, items) -> items.toItemResponses() }
