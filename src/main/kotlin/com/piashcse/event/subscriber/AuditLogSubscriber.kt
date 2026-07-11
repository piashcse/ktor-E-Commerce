package com.piashcse.event.subscriber

import com.piashcse.event.*
import org.slf4j.LoggerFactory

class AuditLogSubscriber : Subscriber {
    private val log = LoggerFactory.getLogger(AuditLogSubscriber::class.java)

    override suspend fun onEvent(event: DomainEvent) {
        val msg = when (event) {
            is OrderPlacedEvent -> "Order ${event.orderNumber} by ${event.userId} for \$${event.total}"
            is UserRegisteredEvent -> "User ${event.email} as ${event.userType}"
            is PaymentCompletedEvent -> "Payment ${event.paymentId} for order ${event.orderId}, \$${event.amount}"
            is SendEmailEvent -> "Email to ${event.to}: ${event.subject}"
        }
        log.info("[AUDIT] $msg")
    }
}
