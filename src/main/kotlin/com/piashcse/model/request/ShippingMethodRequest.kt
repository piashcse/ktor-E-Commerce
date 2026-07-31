package com.piashcse.model.request

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate
import java.math.BigDecimal

@Serializable
data class ShippingMethodRequest(
    val name: String,
    val type: String?,
    @Contextual val price: BigDecimal,
    val deliveryTime: String?,
) {
    init {
        validate(this) {
            validate(ShippingMethodRequest::name).isNotNull().isNotEmpty()
            validate(ShippingMethodRequest::price).isNotNull().isGreaterThan(BigDecimal.ZERO)
        }
    }
}
