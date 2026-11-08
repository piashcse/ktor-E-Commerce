package com.piashcse.service

import com.piashcse.plugin.appMicrometerRegistry
import io.micrometer.core.instrument.Counter

/** Application-level counters for the money and outbox paths. */
object AppMetrics {
    val outboxDelivered: Counter =
        Counter.builder("app.outbox.delivered").description("Outbox events delivered to subscribers").register(appMicrometerRegistry)
    val outboxFailed: Counter =
        Counter.builder("app.outbox.failed").description("Outbox delivery failures").register(appMicrometerRegistry)
    val outboxDeadLettered: Counter =
        Counter.builder(
            "app.outbox.dead_lettered",
        ).description("Outbox rows retired to the dead-letter table").register(appMicrometerRegistry)
    val paymentsCreated: Counter =
        Counter.builder("app.payment.created").description("Payment intents created").register(appMicrometerRegistry)
    val paymentsConfirmed: Counter =
        Counter.builder("app.payment.confirmed").description("Payments confirmed/completed").register(appMicrometerRegistry)
    val refundsCompleted: Counter =
        Counter.builder("app.refund.completed").description("Refunds marked REFUNDED").register(appMicrometerRegistry)
    val refundsRejected: Counter =
        Counter.builder("app.refund.rejected").description("Refunds rejected").register(appMicrometerRegistry)
    val payoutsRequested: Counter =
        Counter.builder("app.payout.requested").description("Seller payout requests").register(appMicrometerRegistry)
    val payoutsPaid: Counter =
        Counter.builder("app.payout.paid").description("Payout rows marked PAID").register(appMicrometerRegistry)
}
