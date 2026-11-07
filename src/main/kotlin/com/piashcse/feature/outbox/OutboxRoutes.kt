package com.piashcse.feature.outbox

import com.piashcse.database.entities.OutboxDeadLetterDAO
import com.piashcse.database.entities.OutboxDeadLetterTable
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.extension.toPaginatedResponse
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SortOrder
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

/** Admin visibility into outbox rows retired after max delivery attempts. */
fun Route.outboxAdminRoutes() {
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
