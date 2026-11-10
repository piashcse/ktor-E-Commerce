package com.piashcse.database.entities

import com.piashcse.constants.PaymentProvider
import com.piashcse.constants.WebhookEventStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.database.entities.base.currentUtc
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

/** Durable inbox for provider webhooks; `(provider, event_id)` is unique. */
object PaymentWebhookEventTable : BaseIdTable("payment_webhook_event") {
    val provider = enumerationByName("provider", 30, PaymentProvider::class)
    val eventId = varchar("event_id", 150)
    val eventType = varchar("event_type", 100)
    val status = enumerationByName("status", 20, WebhookEventStatus::class).default(WebhookEventStatus.RECEIVED)
    val payload = text("payload")
    val error = text("error").nullable()
    val receivedAt = datetime("received_at").clientDefault { currentUtc() }
    val processedAt = datetime("processed_at").nullable()

    init {
        uniqueIndex("payment_webhook_event_provider_event_unique", provider, eventId)
    }
}

class PaymentWebhookEventDAO(id: EntityID<String>) : BaseEntity(id, PaymentWebhookEventTable) {
    companion object : BaseEntityClass<PaymentWebhookEventDAO>(PaymentWebhookEventTable, PaymentWebhookEventDAO::class.java)

    var provider by PaymentWebhookEventTable.provider
    var eventId by PaymentWebhookEventTable.eventId
    var eventType by PaymentWebhookEventTable.eventType
    var status by PaymentWebhookEventTable.status
    var payload by PaymentWebhookEventTable.payload
    var error by PaymentWebhookEventTable.error
    var receivedAt by PaymentWebhookEventTable.receivedAt
    var processedAt by PaymentWebhookEventTable.processedAt
}
