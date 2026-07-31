package com.piashcse.event

import java.math.BigDecimal
import java.time.LocalDateTime

sealed class DomainEvent(
    open val occurredAt: LocalDateTime = LocalDateTime.now(),
    open val requestId: String? = null,
)

data class OrderPlacedEvent(
    val orderId: String,
    val userId: String,
    val email: String,
    val shopId: String?,
    val orderNumber: String,
    val total: BigDecimal,
) : DomainEvent()

data class UserRegisteredEvent(
    val userId: String,
    val email: String,
    val userType: String,
) : DomainEvent()

data class PaymentCompletedEvent(
    val paymentId: String,
    val orderId: String,
    val userId: String,
    val email: String,
    val amount: BigDecimal,
) : DomainEvent()

data class SendEmailEvent(
    val to: String,
    val subject: String,
    val body: String,
) : DomainEvent()
