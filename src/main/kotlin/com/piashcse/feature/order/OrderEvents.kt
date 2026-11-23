package com.piashcse.feature.order

import com.piashcse.constants.OrderStatus
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.event.OrderStatusChangedEvent
import com.piashcse.event.OutboxAggregate
import com.piashcse.event.OutboxPublisher

/**
 * Order lifecycle events shared by checkout, shipping and POS flows.
 *
 * Must run inside the caller's transaction so the event commits atomically
 * with the status change.
 */
object OrderEvents {
    fun statusChangedTx(
        order: OrderDAO,
        from: OrderStatus,
        to: OrderStatus,
        trackingNumber: String? = null,
    ) {
        if (from == to) return
        OutboxPublisher.enqueueTx(
            OutboxAggregate.ORDER,
            order.id.value,
            OrderStatusChangedEvent(
                orderId = order.id.value,
                userId = order.userId.value,
                email = UserDAO.findById(order.userId.value)?.email.orEmpty(),
                orderNumber = order.orderNumber,
                fromStatus = from.name,
                toStatus = to.name,
                trackingNumber = trackingNumber,
            ),
        )
    }
}
