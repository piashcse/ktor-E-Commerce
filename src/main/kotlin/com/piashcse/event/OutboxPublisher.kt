package com.piashcse.event

import com.piashcse.database.entities.OutboxDAO
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

private val outboxJson = Json {
    ignoreUnknownKeys = true
    serializersModule = SerializersModule {
        contextual(com.piashcse.plugin.BigDecimalSerializer)
        contextual(com.piashcse.plugin.LocalDateTimeSerializer)
    }
}

/**
 * Transactional outbox writer. MUST be called inside the caller's DB transaction
 * (no `query {}` wrapper here) so the row commits atomically with business writes.
 */
object OutboxPublisher {
    fun enqueueTx(aggregateType: String, aggregateId: String, event: DomainEvent) {
        val (type, payload) = when (event) {
            is OrderPlacedEvent -> "ORDER_PLACED" to outboxJson.encodeToString(OrderPlacedEvent.serializer(), event)
            is PaymentCompletedEvent -> "PAYMENT_COMPLETED" to outboxJson.encodeToString(PaymentCompletedEvent.serializer(), event)
            is RefundStatusChangedEvent -> "REFUND_STATUS_CHANGED" to outboxJson.encodeToString(RefundStatusChangedEvent.serializer(), event)
            is AdminActionEvent -> "ADMIN_ACTION" to outboxJson.encodeToString(AdminActionEvent.serializer(), event)
            is UserRegisteredEvent -> "USER_REGISTERED" to outboxJson.encodeToString(UserRegisteredEvent.serializer(), event)
            is SendEmailEvent -> "SEND_EMAIL" to outboxJson.encodeToString(SendEmailEvent.serializer(), event)
        }
        OutboxDAO.new {
            this.aggregateType = aggregateType
            this.aggregateId = aggregateId
            this.eventType = type
            this.payload = payload
        }
    }

    fun decode(type: String, payload: String): DomainEvent? = runCatching {
        when (type) {
            "ORDER_PLACED" -> outboxJson.decodeFromString(OrderPlacedEvent.serializer(), payload)
            "PAYMENT_COMPLETED" -> outboxJson.decodeFromString(PaymentCompletedEvent.serializer(), payload)
            "REFUND_STATUS_CHANGED" -> outboxJson.decodeFromString(RefundStatusChangedEvent.serializer(), payload)
            "ADMIN_ACTION" -> outboxJson.decodeFromString(AdminActionEvent.serializer(), payload)
            "USER_REGISTERED" -> outboxJson.decodeFromString(UserRegisteredEvent.serializer(), payload)
            "SEND_EMAIL" -> outboxJson.decodeFromString(SendEmailEvent.serializer(), payload)
            else -> null
        }
    }.getOrNull()
}
