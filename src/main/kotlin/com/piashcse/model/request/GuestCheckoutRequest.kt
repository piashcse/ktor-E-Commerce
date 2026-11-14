package com.piashcse.model.request

import com.piashcse.constants.PaymentMethod
import kotlinx.serialization.Serializable
import org.valiktor.functions.isEmail
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class GuestCheckoutItem(
    val productId: String,
    val quantity: Int,
    val variantId: String? = null,
) {
    init {
        validate(this) {
            validate(GuestCheckoutItem::productId).isNotNull().isNotEmpty()
            validate(GuestCheckoutItem::quantity).isNotNull().isGreaterThan(0)
        }
    }
}

@Serializable
data class GuestCheckoutRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val streetAddress: String,
    val city: String,
    val state: String? = null,
    val country: String,
    val postCode: String,
    val shippingMethodId: String,
    val paymentMethod: PaymentMethod? = null,
    val couponCode: String? = null,
    val currency: String = "USD",
    val notes: String? = null,
    val idempotencyKey: String? = null,
    /** Explicit acceptance when policy consent is enforced. */
    val consentAccepted: Boolean = false,
    val items: List<GuestCheckoutItem>,
) {
    init {
        validate(this) {
            validate(GuestCheckoutRequest::email).isNotNull().isEmail()
            validate(GuestCheckoutRequest::firstName).isNotNull().isNotEmpty()
            validate(GuestCheckoutRequest::lastName).isNotNull().isNotEmpty()
            validate(GuestCheckoutRequest::phoneNumber).isNotNull().isNotEmpty()
            validate(GuestCheckoutRequest::streetAddress).isNotNull().isNotEmpty()
            validate(GuestCheckoutRequest::city).isNotNull().isNotEmpty()
            validate(GuestCheckoutRequest::country).isNotNull().isNotEmpty()
            validate(GuestCheckoutRequest::postCode).isNotNull().isNotEmpty()
            validate(GuestCheckoutRequest::shippingMethodId).isNotNull().isNotEmpty()
            validate(GuestCheckoutRequest::items).isNotNull().isNotEmpty()
        }
    }
}
