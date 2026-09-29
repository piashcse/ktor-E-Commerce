package com.piashcse.event

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.slf4j.LoggerFactory

interface Subscriber {
    suspend fun onEvent(event: DomainEvent)
}

data class DeadLetterEvent(
    val event: DomainEvent,
    val subscriberName: String,
    val lastError: Throwable,
    val attempts: Int,
)

data class EventBusMetrics(
    val published: Long,
    val consumed: Long,
    val failed: Long,
    val deadLetterCount: Int,
)

object EventBus {
    private val log = LoggerFactory.getLogger(EventBus::class.java)
    private val _events = MutableSharedFlow<DomainEvent>(extraBufferCapacity = 256)
    val events = _events.asSharedFlow()
    private val subscribers = java.util.concurrent.CopyOnWriteArrayList<Subscriber>()
    private var job: Job? = null

    private val publishedCount = java.util.concurrent.atomic.AtomicLong(0)
    private val consumedCount = java.util.concurrent.atomic.AtomicLong(0)
    private val failedCount = java.util.concurrent.atomic.AtomicLong(0)
    private val deadLetterCounter = java.util.concurrent.atomic.AtomicInteger(0)

    private val _deadLetter = MutableSharedFlow<DeadLetterEvent>(extraBufferCapacity = 64)
    val deadLetterEvents = _deadLetter.asSharedFlow()

    private const val MAX_RETRIES = 3

    fun metrics(): EventBusMetrics =
        EventBusMetrics(
            published = publishedCount.get(),
            consumed = consumedCount.get(),
            failed = failedCount.get(),
            deadLetterCount = deadLetterCounter.get(),
        )

    fun subscribe(subscriber: Subscriber) {
        if (!subscribers.contains(subscriber)) subscribers.add(subscriber)
    }

    /** Returns false when buffer is full so callers can log instead of silently dropping. */
    fun publish(event: DomainEvent): Boolean {
        val accepted = _events.tryEmit(event)
        if (accepted) {
            publishedCount.incrementAndGet()
        } else {
            log.error("EventBus buffer full — dropped ${event::class.simpleName}")
            failedCount.incrementAndGet()
        }
        return accepted
    }

    /** Shared admin-audit publisher: `actor` is `(id, email, role)`. */
    fun publishAdminAction(
        actor: Triple<String, String, String>,
        action: String,
        resourceType: String,
        resourceId: String?,
        details: String? = null,
    ): Boolean =
        publish(
            AdminActionEvent(
                actorId = actor.first,
                actorEmail = actor.second,
                actorRole = actor.third,
                action = action,
                resourceType = resourceType,
                resourceId = resourceId,
                details = details,
            ),
        )

    fun start(scope: CoroutineScope) {
        job =
            scope.launch {
                events.collect { event ->
                    subscribers.forEach { subscriber ->
                        var lastError: Throwable? = null
                        var attempts = 0
                        for (attempt in 1..MAX_RETRIES) {
                            attempts = attempt
                            try {
                                subscriber.onEvent(event)
                                lastError = null
                                break
                            } catch (e: Exception) {
                                lastError = e
                                log.warn(
                                    "${subscriber::class.simpleName} failed (attempt $attempt/$MAX_RETRIES) " +
                                        "on ${event::class.simpleName}: ${e.message}",
                                )
                                if (attempt < MAX_RETRIES) {
                                    delay(100L * (1L shl (attempt - 1)))
                                }
                            }
                        }
                        if (lastError != null) {
                            failedCount.incrementAndGet()
                            log.error(
                                "${subscriber::class.simpleName} permanently failed on ${event::class.simpleName}" +
                                    " after $MAX_RETRIES attempts",
                                lastError,
                            )
                            _deadLetter.tryEmit(
                                DeadLetterEvent(
                                    event = event,
                                    subscriberName = subscriber::class.simpleName ?: "unknown",
                                    lastError = lastError,
                                    attempts = attempts,
                                ),
                            )
                            deadLetterCounter.incrementAndGet()
                        }
                    }
                    consumedCount.incrementAndGet()
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
