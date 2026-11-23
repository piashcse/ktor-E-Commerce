package com.piashcse.feature.shipping

import com.piashcse.constants.ShipmentStatus
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.ShipmentDAO
import com.piashcse.database.entities.ShipmentTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireOrderAccess
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import java.math.BigDecimal

@Serializable
data class ShipmentAddressRequest(
    val name: String,
    val streetAddress: String,
    val city: String,
    val state: String? = null,
    val zipCode: String,
    val country: String,
    val phone: String? = null,
    val email: String? = null,
)

@Serializable
data class ShipmentParcelRequest(
    @Contextual val length: BigDecimal,
    @Contextual val width: BigDecimal,
    @Contextual val height: BigDecimal,
    val distanceUnit: String = "in",
    @Contextual val weight: BigDecimal,
    val massUnit: String = "lb",
)

@Serializable
data class ShipmentQuoteRequest(
    val origin: ShipmentAddressRequest,
    val destination: ShipmentAddressRequest,
    val parcels: List<ShipmentParcelRequest>,
)

@Serializable
data class PurchaseShipmentRequest(
    val rateId: String,
    /** Empty means every remaining unit on the order ships in this parcel. */
    val items: List<ShipmentItemRequest> = emptyList(),
    /** Optional client retry key; repeated purchases return the original label. */
    val idempotencyKey: String? = null,
)

@Serializable
data class ShipmentItemRequest(
    val orderItemId: String,
    val quantity: Int,
)

@Serializable
data class ShipmentItemResponse(
    val orderItemId: String,
    val quantity: Int,
)

@Serializable
data class ShipmentResponse(
    val id: String,
    val orderId: String,
    val carrier: String,
    val service: String?,
    val trackingNumber: String?,
    val labelUrl: String?,
    val rateAmount: String?,
    val currency: String,
    val status: ShipmentStatus,
    val shippedAt: String?,
    val deliveredAt: String?,
    val createdAt: String?,
    val items: List<ShipmentItemResponse>,
)

@Serializable
data class CarrierWebhookResult(
    val accepted: Boolean,
    val duplicate: Boolean = false,
    val message: String,
)

class ShipmentService(
    private val carrier: CarrierGateway,
) {
    private val webhooks = ShipmentWebhookService(carrier)
    private val labels = ShipmentLabelService(carrier)

    suspend fun quote(
        orderId: String,
        userId: String,
        request: ShipmentQuoteRequest,
    ): List<CarrierRate> {
        query {
            val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
            requireAccess(order, userId)
        }
        if (request.parcels.isEmpty()) throw ValidationException("At least one parcel is required")
        return carrier.quote(
            CarrierQuoteRequest(
                orderId = orderId,
                from = request.origin.toCarrierAddress(),
                to = request.destination.toCarrierAddress(),
                parcels =
                    request.parcels.map {
                        CarrierParcel(
                            length = it.length,
                            width = it.width,
                            height = it.height,
                            distanceUnit = it.distanceUnit,
                            weight = it.weight,
                            massUnit = it.massUnit,
                        )
                    },
            ),
        )
    }

    suspend fun purchaseLabel(
        orderId: String,
        userId: String,
        request: PurchaseShipmentRequest,
    ): ShipmentResponse = labels.purchaseLabel(orderId, userId, request)

    suspend fun list(
        orderId: String,
        userId: String,
    ): List<ShipmentResponse> =
        query {
            val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
            requireAccess(order, userId)
            ShipmentDAO.find { ShipmentTable.orderId eq order.id }
                .orderBy(ShipmentTable.createdAt to SortOrder.ASC)
                .map { it.toShipmentResponse() }
        }

    suspend fun get(
        shipmentId: String,
        userId: String,
    ): ShipmentResponse =
        query {
            val shipment = ShipmentDAO.findById(shipmentId) ?: shipmentId.throwNotFound("Shipment")
            val order = OrderDAO.findById(shipment.orderId.value) ?: shipment.orderId.value.throwNotFound("Order")
            requireAccess(order, userId)
            shipment.toShipmentResponse()
        }

    suspend fun handleWebhook(
        provider: String,
        payload: String,
        signature: String?,
    ): CarrierWebhookResult = webhooks.handleWebhook(provider, payload, signature)

    suspend fun replayWebhook(
        eventId: String,
        actorUserId: String,
    ): CarrierWebhookResult = webhooks.replayWebhook(eventId, actorUserId)

    private fun requireAccess(
        order: OrderDAO,
        userId: String,
    ) {
        val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")
        requireOrderAccess(order.userId.value, order.shopId?.value, userId, user.userType)
    }

    private fun ShipmentAddressRequest.toCarrierAddress() =
        CarrierAddress(
            name = name,
            street1 = streetAddress,
            city = city,
            state = state,
            zip = zipCode,
            country = country,
            phone = phone,
            email = email,
        )
}
