package com.piashcse.event

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList
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

data class EventBusMetrics(
    val published: Long,
    val consumed: Long,
    val failed: Long,
    val deadLetterCount: Int,
)

object EventBus {
    private val log = LoggerFactory.getLogger(EventBus::class.java)
    private val _events = MutableSharedFlow<DomainEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()
    private val subscribers = CopyOnWriteArrayList<Subscriber>()
    private var job: Job? = null

    private val publishedCount = AtomicLong(0)
    private val consumedCount = AtomicLong(0)
    private val failedCount = AtomicLong(0)
    private val deadLetterCount = AtomicLong(0)

    private val _deadLetter = MutableSharedFlow<DeadLetterEvent>(extraBufferCapacity = 64)
    val deadLetterEvents = _deadLetter.asSharedFlow()

    private const val MAX_RETRIES = 3

    // Scope used to drain events that cannot be emitted synchronously (buffer full).
    private val publishScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Deferred publishing: events published inside a DB transaction are buffered
    // on the current thread and only emitted after the surrounding transaction commits.
    private val pendingDepth = ThreadLocal.withInitial { 0 }
    private val pendingEvents = ThreadLocal.withInitial { mutableListOf<DomainEvent>() }

    fun metrics(): EventBusMetrics = EventBusMetrics(
        published = publishedCount.get(),
        consumed = consumedCount.get(),
        failed = failedCount.get(),
        deadLetterCount = deadLetterCount.get().toInt(),
    )

    fun subscribe(subscriber: Subscriber) {
        subscribers.add(subscriber)
    }

    fun publish(event: DomainEvent) {
        if (pendingDepth.get() > 0) {
            pendingEvents.get().add(event)
        } else {
            emit(event)
        }
    }

    private fun emit(event: DomainEvent) {
        if (_events.tryEmit(event)) {
            publishedCount.incrementAndGet()
        } else {
            publishScope.launch {
                _events.emit(event)
                publishedCount.incrementAndGet()
            }
        }
    }

    /** Marks the beginning of a transaction scope; published events are deferred until commit. */
    fun beginAfterCommitScope() {
        pendingDepth.set(pendingDepth.get() + 1)
    }

    /** Flushes events deferred during the transaction after a successful commit. */
    fun commitAfterCommitScope() {
        val depth = pendingDepth.get() - 1
        if (depth == 0) {
            val events = pendingEvents.get()
            pendingEvents.set(mutableListOf())
            pendingDepth.set(0)
            events.forEach { emit(it) }
        } else {
            pendingDepth.set(depth)
        }
    }

    /** Discards events deferred during a rolled-back transaction. */
    fun abortAfterCommitScope() {
        val depth = pendingDepth.get() - 1
        if (depth <= 0) {
            pendingEvents.set(mutableListOf())
            pendingDepth.set(0)
        } else {
            pendingDepth.set(depth)
        }
    }

    fun start(scope: CoroutineScope) {
        job = scope.launch {
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
                        deadLetterCount.incrementAndGet()
                        _deadLetter.tryEmit(
                            DeadLetterEvent(
                                event = event,
                                subscriberName = subscriber::class.simpleName ?: "unknown",
                                lastError = lastError,
                                attempts = attempts,
                            ),
                        )
                    }
                }
                consumedCount.incrementAndGet()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        publishScope.cancel()
    }
}
