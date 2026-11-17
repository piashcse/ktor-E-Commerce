package com.piashcse.event

import com.piashcse.database.entities.base.currentUtc
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
    override val occurredAt: @Contextual LocalDateTime = currentUtc(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class OrderStatusChangedEvent(
    val orderId: String,
    val userId: String,
    val email: String,
    val orderNumber: String,
    val fromStatus: String,
    val toStatus: String,
    val trackingNumber: String? = null,
    override val occurredAt: @Contextual LocalDateTime = currentUtc(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class BackInStockEvent(
    val productId: String,
    val variantId: String? = null,
    override val occurredAt: @Contextual LocalDateTime = currentUtc(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class UserRegisteredEvent(
    val userId: String,
    val email: String,
    val userType: String,
    override val occurredAt: @Contextual LocalDateTime = currentUtc(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class PaymentCompletedEvent(
    val paymentId: String,
    val orderId: String,
    val userId: String,
    val email: String,
    val amount: @Contextual BigDecimal,
    override val occurredAt: @Contextual LocalDateTime = currentUtc(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class SendEmailEvent(
    val to: String,
    val subject: String,
    val body: String,
    override val occurredAt: @Contextual LocalDateTime = currentUtc(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class RmaStatusChangedEvent(
    val rmaId: String,
    val orderId: String,
    val userId: String,
    val email: String,
    val fromStatus: String,
    val toStatus: String,
    val note: String? = null,
    override val occurredAt: @Contextual LocalDateTime = currentUtc(),
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
    override val occurredAt: @Contextual LocalDateTime = currentUtc(),
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
    val reason: String? = null,
    override val occurredAt: @Contextual LocalDateTime = currentUtc(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class SellerReviewCreatedEvent(
    val reviewId: String,
    val sellerId: String,
    val userId: String,
    val rating: Int,
    val comment: String? = null,
    override val occurredAt: @Contextual LocalDateTime = currentUtc(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class ProductReviewCreatedEvent(
    val reviewId: String,
    val productId: String,
    val userId: String,
    val rating: Int,
    val comment: String? = null,
    override val occurredAt: @Contextual LocalDateTime = currentUtc(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class LowStockEvent(
    val productId: String,
    val variantId: String? = null,
    val shopId: String?,
    val stockQuantity: Int,
    override val occurredAt: @Contextual LocalDateTime = currentUtc(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class DisputeCreatedEvent(
    val disputeId: String,
    val sellerId: String?,
    val userId: String,
    val subject: String,
    override val occurredAt: @Contextual LocalDateTime = currentUtc(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class KycStatusChangedEvent(
    val sellerId: String,
    val userId: String,
    val status: String,
    val reason: String? = null,
    override val occurredAt: @Contextual LocalDateTime = currentUtc(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class ShopStatusChangedEvent(
    val shopId: String,
    val userId: String,
    val status: String,
    val reason: String? = null,
    override val occurredAt: @Contextual LocalDateTime = currentUtc(),
    override val requestId: String? = null,
) : DomainEvent()

@Serializable
data class PayoutStatusChangedEvent(
    val payoutRequestId: String,
    val sellerId: String,
    val userId: String,
    val status: String,
    val amount: @Contextual BigDecimal,
    val currency: String = "USD",
    val reason: String? = null,
    override val occurredAt: @Contextual LocalDateTime = currentUtc(),
    override val requestId: String? = null,
) : DomainEvent()
