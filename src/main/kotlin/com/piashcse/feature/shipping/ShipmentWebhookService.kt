package com.piashcse.feature.shipping

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.ShipmentStatus
import com.piashcse.constants.WebhookEventStatus
import com.piashcse.database.entities.CarrierWebhookEventDAO
import com.piashcse.database.entities.CarrierWebhookEventTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.ShipmentDAO
import com.piashcse.database.entities.ShipmentTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.AdminAudit
import com.piashcse.feature.order.OrderEvents
import com.piashcse.service.AppMetrics
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

/** Carrier webhook verification, idempotency and tracking reconciliation. */
class ShipmentWebhookService(
    private val carrier: CarrierGateway,
) {
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
            if (existing != null && existing.status != WebhookEventStatus.FAILED) {
                return@query CarrierWebhookResult(true, duplicate = true, message = "Duplicate webhook ignored")
            }
            val record =
                existing
                    ?: CarrierWebhookEventDAO.new {
                        this.provider = carrier.provider
                        this.eventId = event.eventId
                        this.eventType = event.eventType
                        this.payload = payload
                        this.status = WebhookEventStatus.RECEIVED
                    }
            if (existing != null) {
                record.status = WebhookEventStatus.RECEIVED
                record.error = null
                record.processedAt = null
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
            val accepted = applied || record.status == WebhookEventStatus.IGNORED
            CarrierWebhookResult(
                accepted = accepted,
                message = if (accepted) "Webhook processed" else "Webhook processing failed; retry",
            )
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
            val relevantShipments =
                ShipmentDAO.find { ShipmentTable.orderId eq order.id }
                    .filter { it.status !in listOf(ShipmentStatus.CANCELED, ShipmentStatus.FAILED) }
            val allDelivered =
                relevantShipments.isNotEmpty() && relevantShipments.all { it.status == ShipmentStatus.DELIVERED }
            if (allDelivered && order.status == OrderStatus.SHIPPED) {
                val from = order.status
                order.status = OrderStatus.DELIVERED
                order.deliveredDate = currentUtc()
                OrderEvents.statusChangedTx(order, from, OrderStatus.DELIVERED, order.trackingNumber)
            }
        }
        return true
    }
}
