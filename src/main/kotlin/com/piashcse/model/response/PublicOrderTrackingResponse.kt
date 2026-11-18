package com.piashcse.model.response

import com.piashcse.constants.FulfillmentType
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.PaymentStatus
import com.piashcse.constants.PickupStatus
import kotlinx.serialization.Serializable

/** Public guest-tracking view: no user, contact, address or internal notes. */
@Serializable
data class PublicOrderTrackingResponse(
    val orderNumber: String,
    val status: OrderStatus,
    val paymentStatus: PaymentStatus? = null,
    val total: String,
    val currency: String,
    val itemCount: Int,
    val items: List<PublicOrderItem> = emptyList(),
    val trackingNumber: String? = null,
    val shippingDate: String? = null,
    val deliveredDate: String? = null,
    val canceledDate: String? = null,
    val completedDate: String? = null,
    val fulfillmentType: FulfillmentType = FulfillmentType.DELIVERY,
    val pickupStatus: PickupStatus? = null,
)

@Serializable
data class PublicOrderItem(
    val productName: String? = null,
    val variantName: String? = null,
    val quantity: Int,
    val total: String,
)
