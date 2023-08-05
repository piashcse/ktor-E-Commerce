package com.piashcse.models.orderitem

import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class OrderItem(
    val productId: String,
    val quantity: Int
) {
    fun validation() {
        validate(this) {
            validate(OrderItem::productId).isNotNull().isNotEmpty()
            validate(OrderItem::quantity).isNotNull().isGreaterThan(0)
        }
    }
}
