package com.piashcse.models.shipping

import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate


data class ShippingRequest(
    val orderId: String,
    val shipAddress: String,
    val shipCity: String,
    val shipPhone: Int,
    val shipName: String?,
    val shipEmail: String?,
    val shipCountry: String?
) {
    fun validation() {
        validate(this) {
            validate(ShippingRequest::orderId).isNotNull().isNotEmpty()
            validate(ShippingRequest::shipAddress).isNotNull().isNotEmpty()
            validate(ShippingRequest::shipCity)
            validate(ShippingRequest::shipPhone)
            validate(ShippingRequest::shipEmail).isEmail()
        }
    }
}