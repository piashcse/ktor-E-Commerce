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
    private val _events = MutableSharedFlow<DomainEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()
    private val subscribers = mutableListOf<Subscriber>()
    private var job: Job? = null

    private var publishedCount = 0L
    private var consumedCount = 0L
    private var failedCount = 0L

    private val _deadLetter = MutableSharedFlow<DeadLetterEvent>(extraBufferCapacity = 64)
    val deadLetterEvents = _deadLetter.asSharedFlow()

    private const val MAX_RETRIES = 3

    fun metrics(): EventBusMetrics = EventBusMetrics(
        published = publishedCount,
        consumed = consumedCount,
        failed = failedCount,
        deadLetterCount = _deadLetter.replayCache.size,
    )

    fun subscribe(subscriber: Subscriber) {
        subscribers.add(subscriber)
    }

    fun publish(event: DomainEvent) {
        _events.tryEmit(event)
        publishedCount++
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
                        failedCount++
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
                    }
                }
                consumedCount++
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
