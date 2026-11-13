package com.piashcse.event

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

interface Subscriber {
    suspend fun onEvent(event: DomainEvent)
}

data class DeadLetterEvent(
    val event: DomainEvent,
    val subscriberName: String,
    val lastError: Throwable,
    val attempts: Int,
)

data class PoisonEvent(
    val outboxId: String,
    val eventType: String,
    val aggregateType: String,
    val aggregateId: String,
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

    private data class Envelope(
        val event: DomainEvent,
        val ack: CompletableDeferred<Boolean>? = null,
    )

    private val _events = MutableSharedFlow<Envelope>(extraBufferCapacity = 256)
    private val events = _events.asSharedFlow()
    private val subscribers = CopyOnWriteArrayList<Subscriber>()

    @Volatile
    private var job: Job? = null

    private val publishedCount = AtomicLong(0)
    private val consumedCount = AtomicLong(0)
    private val failedCount = AtomicLong(0)
    private val deadLetterCounter = AtomicInteger(0)

    private val _deadLetter = MutableSharedFlow<DeadLetterEvent>(extraBufferCapacity = 64)
    val deadLetterEvents = _deadLetter.asSharedFlow()

    private val _poisonEvents = MutableSharedFlow<PoisonEvent>(extraBufferCapacity = 64)
    val poisonEvents = _poisonEvents.asSharedFlow()
    private val poisonCounter = AtomicInteger(0)

    fun poisonCount(): Int = poisonCounter.get()

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

    fun isReady(): Boolean = job != null && subscribers.isNotEmpty()

    /** Returns false when the bus is not running or the buffer is full. */
    fun publish(event: DomainEvent): Boolean {
        if (!isReady()) {
            log.error("EventBus not ready — dropped ${event::class.simpleName}")
            failedCount.incrementAndGet()
            return false
        }
        val accepted = _events.tryEmit(Envelope(event))
        if (accepted) {
            publishedCount.incrementAndGet()
        } else {
            log.error("EventBus buffer full — dropped ${event::class.simpleName}")
            failedCount.incrementAndGet()
        }
        return accepted
    }

    /**
     * Publishes and waits until every subscriber has processed the event. The
     * outbox poller uses this so `published_at` is only written after consumers
     * succeed (at-least-once delivery, no early ack).
     */
    suspend fun publishAndAwait(
        event: DomainEvent,
        timeoutMs: Long = 10_000L,
    ): Boolean {
        if (!isReady()) return false
        val ack = CompletableDeferred<Boolean>()
        if (!_events.tryEmit(Envelope(event, ack))) return false
        publishedCount.incrementAndGet()
        return withTimeoutOrNull(timeoutMs) { ack.await() } ?: false
    }

    fun reportPoison(record: PoisonEvent) {
        log.error(
            "Outbox poison event id=${record.outboxId} type=${record.eventType} " +
                "aggregate=${record.aggregateType}:${record.aggregateId} (attempt ${record.attempts})",
        )
        poisonCounter.incrementAndGet()
        failedCount.incrementAndGet()
        _poisonEvents.tryEmit(record)
    }

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
        if (job != null) return
        job =
            scope.launch {
                events.collect { envelope ->
                    var allSucceeded = true
                    subscribers.forEach { subscriber ->
                        var lastError: Throwable? = null
                        var attempts = 0
                        for (attempt in 1..MAX_RETRIES) {
                            attempts = attempt
                            try {
                                subscriber.onEvent(envelope.event)
                                lastError = null
                                break
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Throwable) {
                                lastError = e
                                log.warn(
                                    "${subscriber::class.simpleName} failed (attempt $attempt/$MAX_RETRIES) " +
                                        "on ${envelope.event::class.simpleName}: ${e.message}",
                                )
                                if (attempt < MAX_RETRIES) {
                                    delay(100L * (1L shl (attempt - 1)))
                                }
                            }
                        }
                        if (lastError != null) {
                            allSucceeded = false
                            failedCount.incrementAndGet()
                            log.error(
                                "${subscriber::class.simpleName} permanently failed on ${envelope.event::class.simpleName}" +
                                    " after $MAX_RETRIES attempts",
                                lastError,
                            )
                            _deadLetter.tryEmit(
                                DeadLetterEvent(
                                    event = envelope.event,
                                    subscriberName = subscriber::class.simpleName ?: "unknown",
                                    lastError = lastError,
                                    attempts = attempts,
                                ),
                            )
                            deadLetterCounter.incrementAndGet()
                        }
                    }
                    consumedCount.incrementAndGet()
                    envelope.ack?.complete(allSucceeded)
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
        // Application restart (dev reload / multiple testApplication instances)
        // must not retain stale subscribers from the previous bus.
        subscribers.clear()
    }
}
