package com.piashcse.feature.order

import com.piashcse.constants.OrderStatus
import com.piashcse.database.entities.OrderStatusHistoryDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.UserTable
import com.piashcse.utils.extension.entityID

/** Writes an order status history row inside the caller's transaction. */
internal object OrderStatusHistory {
    fun log(
        orderId: String,
        status: OrderStatus,
        notes: String?,
        userId: String?,
    ) {
        OrderStatusHistoryDAO.new {
            this.orderId = orderId.entityID(OrderTable)
            this.status = status
            this.notes = notes
            this.changedBy = userId?.let { it.entityID(UserTable) }
        }
    }
}
