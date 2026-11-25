package com.piashcse.event

import com.piashcse.database.entities.OutboxDAO
import com.piashcse.plugin.BigDecimalSerializer
import com.piashcse.plugin.LocalDateTimeSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.reflect.KClass

private val outboxJson =
    Json {
        ignoreUnknownKeys = true
        serializersModule =
            SerializersModule {
                contextual(BigDecimalSerializer)
                contextual(LocalDateTimeSerializer)
            }
    }

private class OutboxEventCodec(
    val type: String,
    val eventClass: KClass<out DomainEvent>,
    val serializer: KSerializer<out DomainEvent>,
)

/**
 * Single registry for durable event type names and their serializers. Adding an
 * event means adding one entry here instead of editing two parallel `when`
 * blocks.
 */
private val codecs =
    listOf(
        OutboxEventCodec("ORDER_PLACED", OrderPlacedEvent::class, OrderPlacedEvent.serializer()),
        OutboxEventCodec("ORDER_STATUS_CHANGED", OrderStatusChangedEvent::class, OrderStatusChangedEvent.serializer()),
        OutboxEventCodec("BACK_IN_STOCK", BackInStockEvent::class, BackInStockEvent.serializer()),
        OutboxEventCodec("PAYMENT_COMPLETED", PaymentCompletedEvent::class, PaymentCompletedEvent.serializer()),
        OutboxEventCodec("REFUND_STATUS_CHANGED", RefundStatusChangedEvent::class, RefundStatusChangedEvent.serializer()),
        OutboxEventCodec("RMA_STATUS_CHANGED", RmaStatusChangedEvent::class, RmaStatusChangedEvent.serializer()),
        OutboxEventCodec("ADMIN_ACTION", AdminActionEvent::class, AdminActionEvent.serializer()),
        OutboxEventCodec("USER_REGISTERED", UserRegisteredEvent::class, UserRegisteredEvent.serializer()),
        OutboxEventCodec("SEND_EMAIL", SendEmailEvent::class, SendEmailEvent.serializer()),
        OutboxEventCodec("SELLER_REVIEW_CREATED", SellerReviewCreatedEvent::class, SellerReviewCreatedEvent.serializer()),
        OutboxEventCodec("PRODUCT_REVIEW_CREATED", ProductReviewCreatedEvent::class, ProductReviewCreatedEvent.serializer()),
        OutboxEventCodec("DISPUTE_CREATED", DisputeCreatedEvent::class, DisputeCreatedEvent.serializer()),
        OutboxEventCodec("KYC_STATUS_CHANGED", KycStatusChangedEvent::class, KycStatusChangedEvent.serializer()),
        OutboxEventCodec("SHOP_STATUS_CHANGED", ShopStatusChangedEvent::class, ShopStatusChangedEvent.serializer()),
        OutboxEventCodec("PAYOUT_STATUS_CHANGED", PayoutStatusChangedEvent::class, PayoutStatusChangedEvent.serializer()),
        OutboxEventCodec("LOW_STOCK", LowStockEvent::class, LowStockEvent.serializer()),
        OutboxEventCodec("REVIEW_REPLY", ReviewReplyEvent::class, ReviewReplyEvent.serializer()),
    )

private val codecByType = codecs.associateBy { it.type }
private val codecByClass = codecs.associateBy { it.eventClass }

/**
 * Transactional outbox writer. MUST be called inside the caller's DB transaction
 * (no `query {}` wrapper here) so the row commits atomically with business writes.
 */
object OutboxPublisher {
    fun enqueueTx(
        aggregateType: String,
        aggregateId: String,
        event: DomainEvent,
    ) {
        val codec =
            codecByClass[event::class]
                ?: error("No outbox codec registered for ${event::class.qualifiedName}")
        val payload = encode(codec, event)
        OutboxDAO.new {
            this.aggregateType = aggregateType
            this.aggregateId = aggregateId
            this.eventType = codec.type
            this.payload = payload
        }
    }

    fun decode(
        type: String,
        payload: String,
    ): DomainEvent? =
        runCatching {
            codecByType[type]?.let { codec ->
                @Suppress("UNCHECKED_CAST")
                outboxJson.decodeFromString(codec.serializer as KSerializer<DomainEvent>, payload)
            }
        }.getOrNull()

    @Suppress("UNCHECKED_CAST")
    private fun encode(
        codec: OutboxEventCodec,
        event: DomainEvent,
    ): String = outboxJson.encodeToString(codec.serializer as KSerializer<DomainEvent>, event)
}
