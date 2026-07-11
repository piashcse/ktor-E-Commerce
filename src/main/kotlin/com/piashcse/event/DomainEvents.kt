package com.piashcse.event

import java.time.LocalDateTime

sealed class DomainEvent(open val occurredAt: LocalDateTime = LocalDateTime.now())

data class OrderPlacedEvent(
    val orderId: String,
    val userId: String,
    val shopId: String?,
    val orderNumber: String,
    val total: Double,
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
    val amount: Double,
) : DomainEvent()

data class SendEmailEvent(
    val to: String,
    val subject: String,
    val body: String,
) : DomainEvent()
