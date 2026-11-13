package com.piashcse.feature.shipping

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.ShipmentStatus
import com.piashcse.constants.UserType
import com.piashcse.constants.WebhookEventStatus
import com.piashcse.database.entities.CarrierWebhookEventDAO
import com.piashcse.database.entities.CarrierWebhookEventTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.OrderItemTable
import com.piashcse.database.entities.ShipmentDAO
import com.piashcse.database.entities.ShipmentItemDAO
import com.piashcse.database.entities.ShipmentItemTable
import com.piashcse.database.entities.ShipmentTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.AdminAudit
import com.piashcse.event.OrderStatusChangedEvent
import com.piashcse.event.OutboxPublisher
import com.piashcse.service.AppMetrics
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.requireOrderAccess
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.NotFoundException
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
            if (replay != null) return query { replay.toResponse() }
        }
        val allocation = planAllocation(orderId, userId, request.items)
        val label = carrier.purchaseLabel(CarrierLabelRequest(request.rateId, orderId))
        if (!label.success) {
            throw ValidationException(label.message ?: "Carrier label purchase failed")
        }
        return query {
            // Re-validate after the network call: another parcel may have shipped meanwhile.
            val finalAllocation = planAllocationTx(orderId, userId, request.items)
            val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
            val now = currentUtc()
            val carrierName = label.carrier ?: carrier.provider
            val shipment =
                ShipmentDAO.new {
                    this.orderId = order.id
                    this.carrier = carrierName
                    this.service = label.service
                    this.trackingNumber = label.trackingNumber
                    this.labelUrl = label.labelUrl
                    this.providerReference = label.providerReference
                    this.idempotencyKey = request.idempotencyKey?.takeIf { it.isNotBlank() }
                    this.rateId = request.rateId
                    this.rateAmount = label.amount
                    this.currency = label.currency
                    this.status = ShipmentStatus.LABEL_PURCHASED
                    this.shippedAt = now
                }
            finalAllocation.forEach { line ->
                ShipmentItemDAO.new {
                    this.shipmentId = shipment.id
                    this.orderItemId = line.orderItemId.entityID(OrderItemTable)
                    this.quantity = line.quantity
                }
            }
            if (allUnitsShipped(order)) {
                if (order.status == OrderStatus.PAID) {
                    val from = order.status
                    order.status = OrderStatus.SHIPPED
                    order.shippingDate = now
                    label.trackingNumber?.let { order.trackingNumber = it }
                    enqueueStatusChanged(order, from, OrderStatus.SHIPPED, order.trackingNumber)
                } else {
                    label.trackingNumber?.let { order.trackingNumber = it }
                }
            }
            shipment.toResponse(finalAllocation)
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
    ): CarrierWebhookResult {
        if (!carrier.provider.equals(provider, ignoreCase = true)) {
            throw NotFoundException("Unknown shipping carrier: $provider")
        }
        if (!carrier.verifyWebhook(payload, signature)) {
            throw ValidationException("Invalid carrier webhook signature")
        }
        val event =
            runCatching { carrier.parseWebhook(payload) }
                .getOrElse { throw ValidationException("Invalid carrier webhook payload") }
        AppMetrics.carrierWebhooks.increment()

        return query {
            val existing =
                CarrierWebhookEventDAO.find {
                    (CarrierWebhookEventTable.provider eq carrier.provider) and
                        (CarrierWebhookEventTable.eventId eq event.eventId)
                }.firstOrNull()
            if (existing != null) {
                return@query CarrierWebhookResult(true, duplicate = true, message = "Duplicate webhook ignored")
            }
            val record =
                CarrierWebhookEventDAO.new {
                    this.provider = carrier.provider
                    this.eventId = event.eventId
                    this.eventType = event.eventType
                    this.payload = payload
                    this.status = WebhookEventStatus.RECEIVED
                }
            val applied =
                try {
                    applyTrackingEvent(event)
                } catch (e: Exception) {
                    record.status = WebhookEventStatus.FAILED
                    record.error = e.message
                    record.processedAt = currentUtc()
                    false
                }
            if (applied) {
                record.status = WebhookEventStatus.PROCESSED
                record.processedAt = currentUtc()
            } else if (record.status == WebhookEventStatus.RECEIVED) {
                record.status = WebhookEventStatus.IGNORED
                record.error = "No matching shipment for ${event.trackingNumber ?: event.eventType}"
                record.processedAt = currentUtc()
            }
            CarrierWebhookResult(true, message = "Webhook processed")
        }
    }

    suspend fun replayWebhook(
        eventId: String,
        actorUserId: String,
    ): CarrierWebhookResult =
        query {
            val record =
                CarrierWebhookEventDAO.find {
                    CarrierWebhookEventTable.id eq eventId.entityID(CarrierWebhookEventTable)
                }.forUpdate().firstOrNull() ?: eventId.throwNotFound("Carrier webhook event")
            val event =
                runCatching { carrier.parseWebhook(record.payload) }
                    .getOrElse { error ->
                        record.status = WebhookEventStatus.FAILED
                        record.error = error.message ?: "Invalid stored carrier payload"
                        record.processedAt = currentUtc()
                        AdminAudit.publish(
                            actorUserId,
                            "WEBHOOK_REPLAY_FAILED",
                            "CARRIER_WEBHOOK",
                            eventId,
                            record.error,
                        )
                        return@query CarrierWebhookResult(false, message = "Invalid stored carrier payload")
                    }
            val applied =
                runCatching { applyTrackingEvent(event) }
                    .getOrElse { error ->
                        record.status = WebhookEventStatus.FAILED
                        record.error = error.message
                        record.processedAt = currentUtc()
                        false
                    }
            record.status =
                if (applied) {
                    WebhookEventStatus.PROCESSED
                } else {
                    WebhookEventStatus.IGNORED
                }
            record.error = if (applied) null else "No matching shipment for ${event.trackingNumber ?: event.eventType}"
            record.processedAt = currentUtc()
            AdminAudit.publish(
                actorUserId,
                "WEBHOOK_REPLAYED",
                "CARRIER_WEBHOOK",
                eventId,
                "${event.eventType}: ${record.status}",
            )
            CarrierWebhookResult(true, message = "Carrier webhook replayed")
        }

    private fun applyTrackingEvent(event: CarrierWebhookEvent): Boolean {
        val status = event.status ?: return false
        val shipment =
            event.trackingNumber?.let { tracking ->
                ShipmentDAO.find { ShipmentTable.trackingNumber eq tracking }.forUpdate().firstOrNull()
            } ?: event.providerReference?.let { reference ->
                ShipmentDAO.find { ShipmentTable.providerReference eq reference }.forUpdate().firstOrNull()
            } ?: return false

        if (shipment.status == ShipmentStatus.DELIVERED) return true
        // Keep happy-path statuses monotonic. Terminal exceptions may still
        // override (carrier reports a return/failure after transit).
        val rank =
            mapOf(
                ShipmentStatus.PENDING to 0,
                ShipmentStatus.LABEL_PURCHASED to 1,
                ShipmentStatus.IN_TRANSIT to 2,
                ShipmentStatus.DELIVERED to 3,
            )
        if (status in rank && (rank[shipment.status] ?: -1) > (rank[status] ?: -1)) return true
        shipment.status = status
        if (status == ShipmentStatus.DELIVERED) {
            shipment.deliveredAt = currentUtc()
            val order = OrderDAO.findById(shipment.orderId.value) ?: return true
            val allDelivered =
                ShipmentDAO.find { ShipmentTable.orderId eq order.id }
                    .all { it.status == ShipmentStatus.DELIVERED }
            if (allDelivered && order.status == OrderStatus.SHIPPED) {
                val from = order.status
                order.status = OrderStatus.DELIVERED
                order.deliveredDate = currentUtc()
                enqueueStatusChanged(order, from, OrderStatus.DELIVERED, order.trackingNumber)
            }
        }
        return true
    }

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
        val order = OrderDAO.findById(orderId) ?: orderId.throwNotFound("Order")
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

    private fun enqueueStatusChanged(
        order: OrderDAO,
        from: OrderStatus,
        to: OrderStatus,
        trackingNumber: String?,
    ) {
        if (from == to) return
        OutboxPublisher.enqueueTx(
            "ORDER",
            order.id.value,
            OrderStatusChangedEvent(
                orderId = order.id.value,
                userId = order.userId.value,
                email = UserDAO.findById(order.userId.value)?.email.orEmpty(),
                orderNumber = order.orderNumber,
                fromStatus = from.name,
                toStatus = to.name,
                trackingNumber = trackingNumber,
            ),
        )
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
