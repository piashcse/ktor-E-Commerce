package com.piashcse.feature.order

import com.piashcse.constants.OrderStatus
import com.piashcse.utils.extension.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class ShipOrderRequest(val trackingNumber: String)

@Serializable
data class OrderMilestone(
    val key: String,
    val label: String,
    val occurredAt: String? = null,
)

@Serializable
data class OrderTimelineResponse(
    val orderNumber: String,
    val status: OrderStatus,
    val trackingNumber: String? = null,
    val milestones: List<OrderMilestone>,
)
