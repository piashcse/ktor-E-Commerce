package com.piashcse.database.models.order

import com.piashcse.database.models.orderitem.OrderItemRequest
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class OrderRequest(
    val quantity: Int,
    val subTotal: Float,
    val total: Float,
    val shippingCharge: Float,
    val orderStatus: String,
    val orderItems: MutableList<OrderItemRequest>
) {
    fun validation() {
        validate(this) {
            validate(OrderRequest::quantity).isNotNull().isGreaterThan(0)
            validate(OrderRequest::subTotal).isNotNull().isGreaterThan(0f)
            validate(OrderRequest::total).isNotNull().isGreaterThan(0f)
            validate(OrderRequest::shippingCharge).isNotNull().isGreaterThan(0f)
            validate(OrderRequest::orderStatus).isNotNull()
            validate(OrderRequest::orderItems).isNotNull()
        }
    }
}

