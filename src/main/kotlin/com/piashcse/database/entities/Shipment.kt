package com.piashcse.database.entities

import com.piashcse.constants.ShipmentStatus
import com.piashcse.constants.WebhookEventStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.database.entities.base.currentUtc
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object ShipmentTable : BaseIdTable("shipment") {
    val orderId = reference("order_id", OrderTable.id).index()
    val carrier = varchar("carrier", 50)
    val service = varchar("service", 100).nullable()
    val trackingNumber = varchar("tracking_number", 100).nullable().index()
    val labelUrl = text("label_url").nullable()
    val providerReference = varchar("provider_reference", 150).nullable().index()
    val idempotencyKey = varchar("idempotency_key", 100).nullable()
    val rateId = varchar("rate_id", 150).nullable()
    val rateAmount = decimal("rate_amount", 10, 2).nullable()
    val currency = varchar("currency", 3).default("USD")
    val status = enumerationByName("status", 30, ShipmentStatus::class).default(ShipmentStatus.PENDING)
    val shippedAt = datetime("shipped_at").nullable()
    val deliveredAt = datetime("delivered_at").nullable()
}

class ShipmentDAO(id: EntityID<String>) : BaseEntity(id, ShipmentTable) {
    companion object : BaseEntityClass<ShipmentDAO>(ShipmentTable, ShipmentDAO::class.java)

    var orderId by ShipmentTable.orderId
    var carrier by ShipmentTable.carrier
    var service by ShipmentTable.service
    var trackingNumber by ShipmentTable.trackingNumber
    var labelUrl by ShipmentTable.labelUrl
    var providerReference by ShipmentTable.providerReference
    var idempotencyKey by ShipmentTable.idempotencyKey
    var rateId by ShipmentTable.rateId
    var rateAmount by ShipmentTable.rateAmount
    var currency by ShipmentTable.currency
    var status by ShipmentTable.status
    var shippedAt by ShipmentTable.shippedAt
    var deliveredAt by ShipmentTable.deliveredAt
}

/** Per-order-line allocation so one order can ship in multiple parcels. */
object ShipmentItemTable : BaseIdTable("shipment_item") {
    val shipmentId = reference("shipment_id", ShipmentTable.id).index()
    val orderItemId = reference("order_item_id", OrderItemTable.id).index()
    val quantity = integer("quantity")

    init {
        uniqueIndex("shipment_item_unique", shipmentId, orderItemId)
    }
}

class ShipmentItemDAO(id: EntityID<String>) : BaseEntity(id, ShipmentItemTable) {
    companion object : BaseEntityClass<ShipmentItemDAO>(ShipmentItemTable, ShipmentItemDAO::class.java)

    var shipmentId by ShipmentItemTable.shipmentId
    var orderItemId by ShipmentItemTable.orderItemId
    var quantity by ShipmentItemTable.quantity
}

object CarrierWebhookEventTable : BaseIdTable("carrier_webhook_event") {
    val provider = varchar("provider", 50)
    val eventId = varchar("event_id", 150)
    val eventType = varchar("event_type", 100)
    val status =
        enumerationByName("status", 20, WebhookEventStatus::class)
            .default(WebhookEventStatus.RECEIVED)
    val payload = text("payload")
    val error = text("error").nullable()
    val receivedAt = datetime("received_at").clientDefault { currentUtc() }
    val processedAt = datetime("processed_at").nullable()

    init {
        uniqueIndex("carrier_webhook_event_provider_event_unique", provider, eventId)
    }
}

class CarrierWebhookEventDAO(id: EntityID<String>) : BaseEntity(id, CarrierWebhookEventTable) {
    companion object : BaseEntityClass<CarrierWebhookEventDAO>(CarrierWebhookEventTable, CarrierWebhookEventDAO::class.java)

    var provider by CarrierWebhookEventTable.provider
    var eventId by CarrierWebhookEventTable.eventId
    var eventType by CarrierWebhookEventTable.eventType
    var status by CarrierWebhookEventTable.status
    var payload by CarrierWebhookEventTable.payload
    var error by CarrierWebhookEventTable.error
    var receivedAt by CarrierWebhookEventTable.receivedAt
    var processedAt by CarrierWebhookEventTable.processedAt
}
