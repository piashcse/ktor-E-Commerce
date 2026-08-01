package com.piashcse.event.subscriber

import com.piashcse.event.*
import com.piashcse.utils.email.EmailSender
import com.piashcse.utils.money.Money

class EmailSubscriber : Subscriber {
    override suspend fun onEvent(event: DomainEvent) = when (event) {
        is SendEmailEvent -> EmailSender.send(event.to, event.subject, event.body)
        is UserRegisteredEvent -> EmailSender.send(event.email, "Welcome to Ktor E-Commerce", "Welcome! Your account has been created as ${event.userType}.")
        is OrderPlacedEvent -> EmailSender.send(event.email, "Order Confirmed", "Order ${event.orderNumber} placed. Total: $${Money.plain(event.total)}")
        is PaymentCompletedEvent -> EmailSender.send(event.email, "Payment Received", "Payment of $${Money.plain(event.amount)} for order ${event.orderId} confirmed.")
    }
}
