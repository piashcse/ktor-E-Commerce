package com.piashcse.event

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

@Serializable
sealed class DomainEvent {
    abstract val occurredAt: @Contextual LocalDateTime
    abstract val requestId: String?
}

@Serializable
data class OrderPlacedEvent(
    val orderId: String,
    val userId: String,
    val email: String,
    val shopId: String?,
    val orderNumber: String,
    val total: @Contextual BigDecimal,
    override val occurredAt: @Contextual LocalDateTime = LocalDateTime.now(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class UserRegisteredEvent(
    val userId: String,
    val email: String,
    val userType: String,
    override val occurredAt: @Contextual LocalDateTime = LocalDateTime.now(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class PaymentCompletedEvent(
    val paymentId: String,
    val orderId: String,
    val userId: String,
    val email: String,
    val amount: @Contextual BigDecimal,
    override val occurredAt: @Contextual LocalDateTime = LocalDateTime.now(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class SendEmailEvent(
    val to: String,
    val subject: String,
    val body: String,
    override val occurredAt: @Contextual LocalDateTime = LocalDateTime.now(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class AdminActionEvent(
    val actorId: String,
    val actorEmail: String,
    val actorRole: String,
    val action: String,
    val resourceType: String,
    val resourceId: String?,
    val details: String? = null,
    override val occurredAt: @Contextual LocalDateTime = LocalDateTime.now(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class RefundStatusChangedEvent(
    val refundId: String,
    val orderId: String,
    val userId: String,
    val email: String,
    val fromStatus: String,
    val toStatus: String,
    override val occurredAt: @Contextual LocalDateTime = LocalDateTime.now(),
    override val requestId: String? = null,
) : DomainEvent()
