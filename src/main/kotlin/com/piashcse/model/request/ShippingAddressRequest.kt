package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isEmail
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class ShippingAddressRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val streetAddress: String,
    val city: String,
    val state: String?,
    val country: String,
    val zipCode: String,
    val isDefault: Boolean = false,
) {
    fun validation() {
        validate(this) {
            validate(ShippingAddressRequest::firstName).isNotNull().isNotEmpty()
            validate(ShippingAddressRequest::lastName).isNotNull().isNotEmpty()
            validate(ShippingAddressRequest::email).isNotNull().isEmail()
            validate(ShippingAddressRequest::phoneNumber).isNotNull().isNotEmpty()
            validate(ShippingAddressRequest::streetAddress).isNotNull().isNotEmpty()
            validate(ShippingAddressRequest::city).isNotNull().isNotEmpty()
            validate(ShippingAddressRequest::country).isNotNull().isNotEmpty()
            validate(ShippingAddressRequest::zipCode).isNotNull().isNotEmpty()
        }
    }
}
