package com.piashcse.feature.outbox

import com.piashcse.database.entities.OutboxDAO
import com.piashcse.database.entities.OutboxDeadLetterDAO
import com.piashcse.database.entities.OutboxDeadLetterTable
import com.piashcse.database.entities.OutboxTable
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

@Serializable
data class OutboxDeadLetterResponse(
    val id: String,
    val eventType: String,
    val aggregateType: String,
    val aggregateId: String,
    val payload: String,
    val error: String?,
    val attempts: Int,
    val createdAt: String,
)

@Serializable
data class OutboxSummaryResponse(
    val pending: Long,
    val deadLetters: Long,
    val oldestPendingAt: String? = null,
)

/** Admin visibility and replay for outbox rows retired after max delivery attempts. */
class OutboxService {
    suspend fun summary(): OutboxSummaryResponse =
        query {
            val oldest =
                OutboxTable.selectAll()
                    .where { OutboxTable.publishedAt.isNull() }
                    .orderBy(OutboxTable.createdAt to SortOrder.ASC)
                    .limit(1)
                    .firstOrNull()
                    ?.get(OutboxTable.createdAt)
                    ?.toUtcString()
            OutboxSummaryResponse(
                pending = OutboxTable.selectAll().where { OutboxTable.publishedAt.isNull() }.count(),
                deadLetters = OutboxDeadLetterTable.selectAll().count(),
                oldestPendingAt = oldest,
            )
        }

    suspend fun listDeadLetters(
        limit: Int,
        offset: Int,
        eventType: String? = null,
        aggregateType: String? = null,
    ): PaginatedResponse<OutboxDeadLetterResponse> =
        query {
            OutboxDeadLetterTable.selectAll()
                .also { query ->
                    eventType?.takeIf { it.isNotBlank() }?.let { value ->
                        query.andWhere { OutboxDeadLetterTable.eventType eq value }
                    }
                    aggregateType?.takeIf { it.isNotBlank() }?.let { value ->
                        query.andWhere { OutboxDeadLetterTable.aggregateType eq value }
                    }
                }
                .orderBy(OutboxDeadLetterTable.createdAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { row ->
                    val dao = OutboxDeadLetterDAO.wrapRow(row)
                    OutboxDeadLetterResponse(
                        id = dao.id.value,
                        eventType = dao.eventType,
                        aggregateType = dao.aggregateType,
                        aggregateId = dao.aggregateId,
                        payload = dao.payload,
                        error = dao.error,
                        attempts = dao.attempts,
                        createdAt = dao.createdAt.toUtcString(),
                    )
                }
        }

    suspend fun getDeadLetter(id: String): OutboxDeadLetterResponse =
        query {
            val dao = OutboxDeadLetterDAO.findById(id) ?: id.throwNotFound("Dead letter")
            OutboxDeadLetterResponse(
                id = dao.id.value,
                eventType = dao.eventType,
                aggregateType = dao.aggregateType,
                aggregateId = dao.aggregateId,
                payload = dao.payload,
                error = dao.error,
                attempts = dao.attempts,
                createdAt = dao.createdAt.toUtcString(),
            )
        }

    suspend fun requeue(id: String) {
        query {
            requeueTx(id)
        }
    }

    suspend fun requeueMany(ids: List<String>): Int {
        if (ids.isEmpty()) return 0
        return query {
            ids.distinct().count { id -> requeueTx(id) }
        }
    }

    private fun requeueTx(id: String): Boolean {
        val deadLetter = OutboxDeadLetterDAO.findById(id) ?: return false
        OutboxDAO.new {
            aggregateType = deadLetter.aggregateType
            aggregateId = deadLetter.aggregateId
            eventType = deadLetter.eventType
            payload = deadLetter.payload
        }
        deadLetter.delete()
        return true
    }
}
