package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ShippingAddressResponse(
    val id: String,
    val userId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val streetAddress: String,
    val city: String,
    val state: String?,
    val country: String,
    val zipCode: String,
    val isDefault: Boolean
)
