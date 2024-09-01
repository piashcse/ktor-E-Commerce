package com.piashcse.models.order

import com.piashcse.utils.extension.OrderStatus
import org.valiktor.functions.isIn
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class UpdateOrder(val orderStatus: String) {
    fun validation() {
        validate(this) {
            validate(UpdateOrder::orderStatus).isNotNull().isIn(
                OrderStatus.PENDING.name.lowercase(),
                OrderStatus.CONFIRMED.name.lowercase(),
                OrderStatus.PAID.name.lowercase(),
                OrderStatus.DELIVERED.name.lowercase(),
                OrderStatus.CANCELED.name.lowercase(),
                OrderStatus.RECEIVED.name.lowercase()
            )
        }
    }
}

data class OrderId(val orderId: String) {
    fun validation() {
        validate(this) {
            validate(OrderId::orderId).isNotNull()
        }
    }
}