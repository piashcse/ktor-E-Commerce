package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class ShippingMethodRequest(
    val name: String,
    val type: String?,
    val price: Double,
    val deliveryTime: String?
) {
    fun validation() {
        validate(this) {
            validate(ShippingMethodRequest::name).isNotNull().isNotEmpty()
            validate(ShippingMethodRequest::price).isNotNull().isGreaterThan(0.0)
        }
    }
}
