package com.piashcse.service

import com.piashcse.database.entities.OutboxDAO
import com.piashcse.database.entities.OutboxTable
import com.piashcse.event.EventBus
import com.piashcse.event.OutboxPublisher
import com.piashcse.utils.extension.query
import io.ktor.server.application.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.v1.core.isNull
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Polls `outbox` for unpublished rows and relays them to the in-memory bus. At-least-once. */
object OutboxPoller {
    private val log = LoggerFactory.getLogger(OutboxPoller::class.java)
    private var job: Job? = null

    fun start(
        scope: CoroutineScope,
        intervalMs: Long = 5_000L,
        batchSize: Int = 50,
    ) {
        if (job != null) return
        job =
            scope.launch(Dispatchers.IO) {
                while (isActive) {
                    runCatching { drain(batchSize) }.onFailure { log.warn("Outbox drain failed: ${it.message}") }
                    delay(intervalMs)
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    suspend fun drain(batchSize: Int = 50): Int =
        query {
            val pending =
                OutboxDAO.find { OutboxTable.publishedAt.isNull() }
                    .limit(batchSize).toList()
            var delivered = 0
            pending.forEach { row ->
                val event = OutboxPublisher.decode(row.eventType, row.payload)
                if (event == null) {
                    row.publishedAt = LocalDateTime.now(ZoneOffset.UTC) // poison — skip
                    return@forEach
                }
                if (EventBus.publish(event)) {
                    row.publishedAt = LocalDateTime.now(ZoneOffset.UTC)
                    delivered++
                } else {
                    row.attempts = row.attempts + 1
                }
            }
            delivered
        }
}

fun Application.configureOutboxPoller() = OutboxPoller.start(this)
