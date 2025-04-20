package com.piashcse.database.models.orderitem

import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class OrderItemRequest(
    val productId: String,
    val quantity: Int
) {
    fun validation() {
        validate(this) {
            validate(OrderItemRequest::productId).isNotNull().isNotEmpty()
            validate(OrderItemRequest::quantity).isNotNull().isGreaterThan(0)
        }
    }
}
