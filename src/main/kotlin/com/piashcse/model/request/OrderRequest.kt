package com.piashcse.model.request

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotNull
import org.valiktor.validate
import java.math.BigDecimal

@Serializable
data class OrderRequest(
    val quantity: Int,
    @Contextual val subTotal: BigDecimal,
    @Contextual val total: BigDecimal,
    @Contextual val shippingCharge: BigDecimal,
    val orderStatus: String,
    val shippingAddress: String,
    val orderItems: MutableList<OrderItemRequest>,
) {
    init {
        validate(this) {
            validate(OrderRequest::quantity).isNotNull().isGreaterThan(0)
            validate(OrderRequest::subTotal).isNotNull().isGreaterThan(BigDecimal.ZERO)
            validate(OrderRequest::total).isNotNull().isGreaterThan(BigDecimal.ZERO)
            validate(OrderRequest::shippingCharge).isNotNull().isGreaterThan(BigDecimal.ZERO)
            validate(OrderRequest::orderStatus).isNotNull()
            validate(OrderRequest::shippingAddress).isNotNull()
            validate(OrderRequest::orderItems).isNotNull()
        }
    }
}
