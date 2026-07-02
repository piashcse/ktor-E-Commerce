package com.piashcse.model.request

import com.piashcse.constants.PaymentMethod
import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class CheckoutRequest(
    val shippingAddressId: String,
    val shippingMethodId: String,
    val paymentMethod: PaymentMethod? = null,
    val notes: String? = null,
    val idempotencyKey: String? = null,
    val couponCode: String? = null,
) {
    init {
        validate(this) {
            validate(CheckoutRequest::shippingAddressId).isNotNull().isNotEmpty()
            validate(CheckoutRequest::shippingMethodId).isNotNull().isNotEmpty()
        }
    }
}
