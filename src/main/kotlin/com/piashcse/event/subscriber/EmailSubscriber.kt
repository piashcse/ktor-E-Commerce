package com.piashcse.event.subscriber

import com.piashcse.event.DomainEvent
import com.piashcse.event.SendEmailEvent
import com.piashcse.event.Subscriber
import com.piashcse.utils.email.EmailSender

/**
 * Sends explicitly queued emails (OTP, password reset, admin messages). Domain
 * notifications are fanned out by [NotificationSubscriber] through the channel
 * dispatcher so templates and per-event preferences are respected.
 */
class EmailSubscriber : Subscriber {
    override suspend fun onEvent(event: DomainEvent) {
        if (event is SendEmailEvent) {
            EmailSender.send(event.to, event.subject, event.body)
        }
    }
}
