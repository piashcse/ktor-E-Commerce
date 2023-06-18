package com.piashcse.models.orderitem

import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class OrderItem(
    val orderId: String,
    val productId: String,
    val totalPrice: Float,
    val singlePrice: Float,
    val quantity: Int
) {
    fun validation() {
        validate(this) {
            validate(OrderItem::productId).isNotNull().isNotEmpty()
            validate(OrderItem::totalPrice).isNotNull().isGreaterThan(0f)
            validate(OrderItem::singlePrice).isNotNull().isGreaterThan(0f)
            validate(OrderItem::quantity).isNotNull().isGreaterThan(0)
        }
    }
}
