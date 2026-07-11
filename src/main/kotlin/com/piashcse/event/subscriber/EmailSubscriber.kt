package com.piashcse.event.subscriber

import com.piashcse.event.*
import com.piashcse.utils.email.EmailSender

class EmailSubscriber : Subscriber {
    override suspend fun onEvent(event: DomainEvent) = when (event) {
        is SendEmailEvent -> EmailSender.send(event.to, event.subject, event.body)
        is UserRegisteredEvent -> EmailSender.send(event.email, "Welcome to Ktor E-Commerce", "Welcome! Your account has been created as ${event.userType}.")
        is OrderPlacedEvent -> EmailSender.send(event.userId, "Order Confirmed", "Order ${event.orderNumber} placed. Total: \$${event.total}")
        is PaymentCompletedEvent -> EmailSender.send(event.userId, "Payment Received", "Payment of \$${event.amount} for order ${event.orderId} confirmed.")
    }
}
