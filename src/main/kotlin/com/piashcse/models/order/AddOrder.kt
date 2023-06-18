package com.piashcse.models.order

import com.piashcse.models.orderitem.OrderItem
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddOrder(
    val quantity: Int,
    val subTotal: Float,
    val total: Float,
    val shippingCharge: Float,
    val orderStatus: String,
    val orderItems: MutableList<OrderItem>
) {
    fun validation() {
        validate(this) {
            validate(AddOrder::quantity).isNotNull().isGreaterThan(0)
            validate(AddOrder::subTotal).isNotNull().isGreaterThan(0f)
            validate(AddOrder::total).isNotNull().isGreaterThan(0f)
            validate(AddOrder::shippingCharge).isNotNull().isGreaterThan(0f)
            validate(AddOrder::orderStatus).isNotNull()
            validate(AddOrder::orderItems).isNotNull()
        }
    }
}

