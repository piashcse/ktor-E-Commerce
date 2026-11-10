package com.piashcse.feature.outbox

import com.piashcse.database.entities.OutboxDAO
import com.piashcse.database.entities.OutboxDeadLetterDAO
import com.piashcse.database.entities.OutboxDeadLetterTable
import com.piashcse.database.entities.OutboxTable
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ValidationException
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.isNull
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
)

/** Admin visibility into outbox rows retired after max delivery attempts. */
fun Route.outboxAdminRoutes() {
    get("summary") {
        val summary =
            com.piashcse.utils.extension.query {
                OutboxSummaryResponse(
                    pending = OutboxTable.selectAll().where { OutboxTable.publishedAt.isNull() }.count(),
                    deadLetters = OutboxDeadLetterTable.selectAll().count(),
                )
            }
        call.respondOk(summary)
    }

    post("dead-letters/{id}/requeue") {
        val id =
            call.parameters["id"]?.takeIf { it.isNotBlank() }
                ?: throw ValidationException("id is required")
        com.piashcse.utils.extension.query {
            val deadLetter = OutboxDeadLetterDAO.findById(id) ?: id.throwNotFound("Dead letter")
            OutboxDAO.new {
                aggregateType = deadLetter.aggregateType
                aggregateId = deadLetter.aggregateId
                eventType = deadLetter.eventType
                payload = deadLetter.payload
            }
            deadLetter.delete()
        }
        call.respondOk(MessageResponse("Dead letter requeued"))
    }

    get("dead-letters") {
        val (limit, offset) = call.paginateQueryParams()
        val page =
            com.piashcse.utils.extension.query {
                OutboxDeadLetterTable.selectAll()
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
        call.respondOk(page)
    }
}
