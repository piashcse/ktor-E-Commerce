package com.piashcse.service

import com.piashcse.database.entities.OutboxDAO
import com.piashcse.database.entities.OutboxDeadLetterDAO
import com.piashcse.database.entities.OutboxTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.event.EventBus
import com.piashcse.event.OutboxPublisher
import com.piashcse.event.PoisonEvent
import com.piashcse.utils.extension.query
import io.ktor.server.application.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.slf4j.LoggerFactory

/** Polls `outbox` for unpublished rows and relays them to the in-memory bus. At-least-once. */
object OutboxPoller {
    private val log = LoggerFactory.getLogger(OutboxPoller::class.java)
    private var job: Job? = null
    private const val MAX_ATTEMPTS = 10

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

    private data class PendingRow(
        val id: String,
        val eventType: String,
        val payload: String,
        val aggregateType: String,
        val aggregateId: String,
        val attempts: Int,
    )

    private fun deadLetter(
        row: OutboxDAO,
        error: String,
    ) {
        AppMetrics.outboxDeadLettered.increment()
        OutboxDeadLetterDAO.new {
            eventType = row.eventType
            aggregateType = row.aggregateType
            aggregateId = row.aggregateId
            payload = row.payload
            this.error = error
            attempts = row.attempts
        }
    }

    suspend fun drain(batchSize: Int = 50): Int {
        // Read the batch in one short transaction. Delivery happens outside the
        // transaction because it waits for subscribers; the row is acknowledged
        // in a second transaction only after consumers succeed.
        val pending =
            query {
                OutboxDAO.find {
                    (OutboxTable.publishedAt.isNull()) and (OutboxTable.attempts less MAX_ATTEMPTS)
                }
                    .orderBy(OutboxTable.createdAt to SortOrder.ASC)
                    .limit(batchSize).toList()
                    .map { row ->
                        PendingRow(
                            id = row.id.value,
                            eventType = row.eventType,
                            payload = row.payload,
                            aggregateType = row.aggregateType,
                            aggregateId = row.aggregateId,
                            attempts = row.attempts,
                        )
                    }
            }

        var delivered = 0
        pending.forEach { pendingRow ->
            val event = OutboxPublisher.decode(pendingRow.eventType, pendingRow.payload)
            if (event == null) {
                query {
                    OutboxDAO.findById(pendingRow.id)?.let { row ->
                        row.attempts = row.attempts + 1
                        if (row.attempts >= MAX_ATTEMPTS) {
                            deadLetter(row, "Undecodable event payload after ${row.attempts} attempts")
                            row.publishedAt = currentUtc()
                            log.error("Outbox row ${row.id.value} retired after ${row.attempts} undecodable attempts")
                        }
                    }
                }
                EventBus.reportPoison(
                    PoisonEvent(
                        outboxId = pendingRow.id,
                        eventType = pendingRow.eventType,
                        aggregateType = pendingRow.aggregateType,
                        aggregateId = pendingRow.aggregateId,
                        attempts = pendingRow.attempts + 1,
                    ),
                )
                return@forEach
            }

            val processed = EventBus.publishAndAwait(event)
            query {
                OutboxDAO.findById(pendingRow.id)?.let { row ->
                    if (processed) {
                        row.publishedAt = currentUtc()
                        AppMetrics.outboxDelivered.increment()
                        delivered++
                    } else {
                        AppMetrics.outboxFailed.increment()
                        row.attempts = row.attempts + 1
                        if (row.attempts >= MAX_ATTEMPTS) {
                            deadLetter(row, "Subscriber delivery failed after ${row.attempts} attempts")
                            row.publishedAt = currentUtc()
                            log.error("Outbox row ${row.id.value} retired after ${row.attempts} delivery failures")
                        }
                    }
                }
            }
        }
        return delivered
    }
}

fun Application.configureOutboxPoller() = OutboxPoller.start(this)
