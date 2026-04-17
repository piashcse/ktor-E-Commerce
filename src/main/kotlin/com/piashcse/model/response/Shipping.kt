package com.piashcse.model.response

import com.piashcse.database.entities.ShippingTable
import kotlinx.serialization.Serializable

@Serializable
data class Shipping(
    val id: String,
    var orderId: String,
    var status: ShippingTable.ShippingStatus,
    var address: String,
    var city: String,
    var country: String?,
    var phone: String,
    var email: String?,
    var shippingMethod: String?,
    var trackingNumber: String?,
)
