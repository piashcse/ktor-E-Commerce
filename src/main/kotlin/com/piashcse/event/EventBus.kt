package com.piashcse.event

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.slf4j.LoggerFactory

interface Subscriber {
    suspend fun onEvent(event: DomainEvent)
}

object EventBus {
    private val log = LoggerFactory.getLogger(EventBus::class.java)
    private val _events = MutableSharedFlow<DomainEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()
    private val subscribers = mutableListOf<Subscriber>()
    private var job: Job? = null

    fun subscribe(subscriber: Subscriber) {
        subscribers.add(subscriber)
    }

    fun publish(event: DomainEvent) {
        _events.tryEmit(event)
    }

    fun start(scope: CoroutineScope) {
        job = scope.launch {
            events.collect { event ->
                subscribers.forEach { subscriber ->
                    runCatching { subscriber.onEvent(event) }
                        .onFailure { log.error("${subscriber::class.simpleName} failed on ${event::class.simpleName}", it) }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
