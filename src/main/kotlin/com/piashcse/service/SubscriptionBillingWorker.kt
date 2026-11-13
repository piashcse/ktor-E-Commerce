package com.piashcse.service

import com.piashcse.feature.subscription.SubscriptionService
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Daily subscription renewal/dunning run. The admin endpoint remains available
 * for manual runs, but billing must not depend on an operator.
 */
object SubscriptionBillingWorker {
    private val log = LoggerFactory.getLogger(SubscriptionBillingWorker::class.java)
    private var job: Job? = null

    fun start(
        scope: CoroutineScope,
        interval: Duration = Duration.ofHours(24),
    ) {
        if (job != null) return
        job =
            scope.launch(Dispatchers.IO) {
                while (isActive) {
                    runCatching { SubscriptionService().runBilling() }
                        .onFailure { log.error("Subscription billing run failed", it) }
                    delay(interval.toMillis())
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
