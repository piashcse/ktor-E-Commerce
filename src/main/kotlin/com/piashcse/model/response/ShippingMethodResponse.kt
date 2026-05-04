package com.piashcse.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ShippingMethodResponse(
    val id: String,
    val name: String,
    val type: String?,
    val price: Double,
    val deliveryTime: String?
)
