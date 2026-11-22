package com.piashcse.feature.shipping

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.ShipmentStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.OrderItemTable
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.ShipmentDAO
import com.piashcse.database.entities.ShipmentItemDAO
import com.piashcse.database.entities.ShipmentItemTable
import com.piashcse.database.entities.ShipmentTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.feature.order.OrderEvents
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.db.isUniqueViolation
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireOrderAccess
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.notInList
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

    private data class Allocation(
        val orderItemId: String,
        val quantity: Int,
    )

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
    ): ShipmentResponse {
        if (request.rateId.isBlank()) throw ValidationException("rateId is required")
        val carrierProvider = carrier.provider
        request.idempotencyKey?.takeIf { it.isNotBlank() }?.let { key ->
            val replay =
                query {
                    val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
                    val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")
                    if (user.userType !in listOf(UserType.SELLER, UserType.ADMIN, UserType.SUPER_ADMIN)) {
                        throw ForbiddenException("Only sellers and admins can purchase shipping labels")
                    }
                    requireOrderAccess(order.userId.value, order.shopId?.value, userId, user.userType)
                    ShipmentDAO.find {
                        (ShipmentTable.orderId eq order.id) and (ShipmentTable.idempotencyKey eq key)
                    }.firstOrNull()
                }
            if (replay != null && replay.status != ShipmentStatus.FAILED) {
                return query { replay.toResponse() }
            }
            if (replay != null && replay.status == ShipmentStatus.FAILED) {
                // Retry the carrier with the same key: remove the failed row so
                // the unique (order_id, idempotency_key) index does not return
                // the old failure as a 201 success.
                query { replay.delete() }
            }
        }

        // Reserve the allocation before calling the carrier. A PENDING shipment
        // counts against shipped quantities, so concurrent purchases cannot
        // allocate the same units; a carrier failure marks it FAILED and frees them.
        val (shipment, allocation) =
            try {
                query {
                    val finalAllocation = planAllocationTx(orderId, userId, request.items)
                    val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
                    val reservation =
                        ShipmentDAO.new {
                            this.orderId = order.id
                            this.carrier = carrierProvider
                            this.rateId = request.rateId
                            this.idempotencyKey = request.idempotencyKey?.takeIf { it.isNotBlank() }
                            this.status = ShipmentStatus.PENDING
                            this.currency = order.currency
                        }
                    finalAllocation.forEach { line ->
                        ShipmentItemDAO.new {
                            this.shipmentId = reservation.id
                            this.orderItemId = line.orderItemId.entityID(OrderItemTable)
                            this.quantity = line.quantity
                        }
                    }
                    reservation to finalAllocation
                }
            } catch (error: Exception) {
                if (error.isUniqueViolation() && request.idempotencyKey != null) {
                    query {
                        ShipmentDAO.find {
                            (ShipmentTable.orderId eq orderId.entityID(OrderTable)) and
                                (ShipmentTable.idempotencyKey eq request.idempotencyKey)
                        }.firstOrNull()
                    }?.let { return it.toResponse() }
                }
                throw error
            }

        val label =
            try {
                carrier.purchaseLabel(CarrierLabelRequest(request.rateId, orderId))
            } catch (error: Exception) {
                query { shipment.status = ShipmentStatus.FAILED }
                throw error
            }
        if (!label.success) {
            query { shipment.status = ShipmentStatus.FAILED }
            throw ValidationException(label.message ?: "Carrier label purchase failed")
        }

        return query {
            val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
            val now = currentUtc()
            shipment.carrier = label.carrier ?: carrier.provider
            shipment.service = label.service
            shipment.trackingNumber = label.trackingNumber
            shipment.labelUrl = label.labelUrl
            shipment.providerReference = label.providerReference
            shipment.rateAmount = label.amount
            shipment.currency = label.currency
            shipment.status = ShipmentStatus.LABEL_PURCHASED
            shipment.shippedAt = now
            if (allUnitsShipped(order)) {
                if (order.status == OrderStatus.PAID) {
                    val from = order.status
                    order.status = OrderStatus.SHIPPED
                    order.shippingDate = now
                    label.trackingNumber?.let { order.trackingNumber = it }
                    OrderEvents.statusChangedTx(order, from, OrderStatus.SHIPPED, order.trackingNumber)
                } else {
                    label.trackingNumber?.let { order.trackingNumber = it }
                }
            }
            shipment.toResponse(allocation)
        }
    }

    suspend fun list(
        orderId: String,
        userId: String,
    ): List<ShipmentResponse> =
        query {
            val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
            requireAccess(order, userId)
            ShipmentDAO.find { ShipmentTable.orderId eq order.id }
                .orderBy(ShipmentTable.createdAt to SortOrder.ASC)
                .map { it.toResponse() }
        }

    suspend fun get(
        shipmentId: String,
        userId: String,
    ): ShipmentResponse =
        query {
            val shipment = ShipmentDAO.findById(shipmentId) ?: shipmentId.throwNotFound("Shipment")
            val order = OrderDAO.findById(shipment.orderId.value) ?: shipment.orderId.value.throwNotFound("Order")
            requireAccess(order, userId)
            shipment.toResponse()
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

    private suspend fun planAllocation(
        orderId: String,
        userId: String,
        requested: List<ShipmentItemRequest>,
    ): List<Allocation> = query { planAllocationTx(orderId, userId, requested) }

    private fun planAllocationTx(
        orderId: String,
        userId: String,
        requested: List<ShipmentItemRequest>,
    ): List<Allocation> {
        // Lock the order before reading shipment quantities so concurrent label
        // purchases cannot allocate the same units.
        val order =
            OrderDAO.find { OrderTable.id eq orderId.entityID(OrderTable) }
                .forUpdate()
                .firstOrNull()
                ?: orderId.throwNotFound("Order")
        val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")
        if (user.userType !in listOf(UserType.SELLER, UserType.ADMIN, UserType.SUPER_ADMIN)) {
            throw ForbiddenException("Only sellers and admins can purchase shipping labels")
        }
        requireOrderAccess(order.userId.value, order.shopId?.value, userId, user.userType)
        if (order.status !in listOf(OrderStatus.PAID, OrderStatus.SHIPPED)) {
            throw ValidationException("Order must be paid before shipping labels can be purchased")
        }
        val items = OrderItemDAO.itemsForOrder(order.id)
        if (items.isEmpty()) throw ValidationException("Order has no items to ship")
        val shipped = shippedQuantities(order.id)
        val remaining =
            items.associate { item ->
                item.id.value to (item.quantity - (shipped[item.id.value] ?: 0))
            }
        val allocation =
            if (requested.isEmpty()) {
                remaining.filterValues { it > 0 }.map { Allocation(it.key, it.value) }
            } else {
                // Merge duplicate lines first; two 5-unit lines against 5
                // remaining must fail before a label is purchased.
                val merged = requested.groupBy { it.orderItemId }.map { (id, lines) -> id to lines.sumOf { it.quantity } }
                merged.map { (orderItemId, quantity) ->
                    if (quantity <= 0) throw ValidationException("Shipment quantity must be positive")
                    val available =
                        remaining[orderItemId]
                            ?: throw ValidationException("Order item $orderItemId is not part of this order")
                    if (quantity > available) {
                        throw ValidationException(
                            "Cannot ship $quantity of $orderItemId; only $available remaining",
                        )
                    }
                    Allocation(orderItemId, quantity)
                }
            }
        if (allocation.isEmpty()) throw ValidationException("All order items have already shipped")
        return allocation
    }

    private fun shippedQuantities(orderId: org.jetbrains.exposed.v1.core.dao.id.EntityID<String>): Map<String, Int> {
        val shipmentIds =
            ShipmentDAO.find {
                (ShipmentTable.orderId eq orderId) and
                    (ShipmentTable.status notInList listOf(ShipmentStatus.CANCELED, ShipmentStatus.FAILED))
            }.map { it.id.value }
        if (shipmentIds.isEmpty()) return emptyMap()
        return ShipmentItemDAO.find { ShipmentItemTable.shipmentId inList shipmentIds }
            .groupBy { it.orderItemId.value }
            .mapValues { (_, rows) -> rows.sumOf { it.quantity } }
    }

    private fun allUnitsShipped(order: OrderDAO): Boolean {
        val items = OrderItemDAO.itemsForOrder(order.id)
        val shipped = shippedQuantities(order.id)
        return items.all { (shipped[it.id.value] ?: 0) >= it.quantity }
    }

    private fun requireAccess(
        order: OrderDAO,
        userId: String,
    ) {
        val user = UserDAO.findById(userId) ?: userId.throwNotFound("User")
        requireOrderAccess(order.userId.value, order.shopId?.value, userId, user.userType)
    }

    private fun ShipmentDAO.toResponse(items: List<Allocation>? = null): ShipmentResponse {
        val lines =
            items ?: ShipmentItemDAO.find { ShipmentItemTable.shipmentId eq id }
                .map { Allocation(it.orderItemId.value, it.quantity) }
        return ShipmentResponse(
            id = id.value,
            orderId = orderId.value,
            carrier = carrier,
            service = service,
            trackingNumber = trackingNumber,
            labelUrl = labelUrl,
            rateAmount = rateAmount?.let { Money.str(it) },
            currency = currency,
            status = status,
            shippedAt = shippedAt?.toUtcString(),
            deliveredAt = deliveredAt?.toUtcString(),
            createdAt = createdAt.toUtcString(),
            items = lines.map { ShipmentItemResponse(it.orderItemId, it.quantity) },
        )
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
