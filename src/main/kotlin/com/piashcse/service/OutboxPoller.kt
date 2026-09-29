package com.piashcse.service

import com.piashcse.database.entities.OutboxDAO
import com.piashcse.database.entities.OutboxTable
import com.piashcse.event.EventBus
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.PoisonEvent
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
            // Claim the next batch under FOR UPDATE in id order so redeliveries are FIFO.
            // Exposed 1.5's DAO finder exposes plain FOR UPDATE only (no SKIP LOCKED), so
            // concurrent pollers would contend on the same head rows: run a single poller
            // instance; do not scale horizontally without an external claim mechanism.
            val pending =
                OutboxDAO.find { OutboxTable.publishedAt.isNull() }
                    .forUpdate()
                    .limit(batchSize).toList()
                    .sortedBy { it.id.value }
            var delivered = 0
            pending.forEach { row ->
                val event = OutboxPublisher.decode(row.eventType, row.payload)
                if (event == null) {
                    // Poison record: undecodable payload. Never mark published-and-dropped —
                    // keep the row unpublished for operator inspection, bump attempts, and
                    // surface it on the dead-letter channel.
                    row.attempts = row.attempts + 1
                    EventBus.reportPoison(
                        PoisonEvent(
                            outboxId = row.id.value,
                            eventType = row.eventType,
                            aggregateType = row.aggregateType,
                            aggregateId = row.aggregateId,
                            attempts = row.attempts,
                        ),
                    )
                    return@forEach
                }
                // At-least-once relay: the row is marked published only after the bus accepts
                // the event. True exactly-once is not possible here — the in-memory bus has no
                // transactional consume, so a crash between publish and commit can redeliver;
                // downstream subscribers must stay idempotent.
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
