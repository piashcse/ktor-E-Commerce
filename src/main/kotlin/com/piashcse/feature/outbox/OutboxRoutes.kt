@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

@file:Suppress("MatchingDeclarationName")

package com.piashcse.feature.outbox

import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.utils.extension.pageRequest
import com.piashcse.utils.extension.respondMessage
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class RequeueDeadLettersRequest(val ids: List<String>)

/** Admin visibility into outbox rows retired after max delivery attempts. */
fun Route.outboxAdminRoutes() {
    val service: OutboxService by inject()

    get("summary") {
        call.respondOk(service.summary())
    }.describe {
        summary = "Outbox summary"
        description = "Pending rows and dead-letter count"
        responses { ok<OutboxSummaryResponse>("Outbox health") }
    }

    post("dead-letters/{id}/requeue") {
        service.requeue(call.requirePathParameter("id"))
        call.respondMessage("Dead letter requeued")
    }.describe {
        summary = "Requeue dead letter"
        description = "Copies the payload back into the outbox"
    }

    post("dead-letters/requeue") {
        val body = call.receive<RequeueDeadLettersRequest>()
        val count = service.requeueMany(body.ids)
        call.respondMessage("$count dead letter(s) requeued")
    }.describe {
        summary = "Bulk requeue dead letters"
        requestBody { jsonBody<RequeueDeadLettersRequest>() }
    }

    get("dead-letters/{id}") {
        call.respondOk(service.getDeadLetter(call.requirePathParameter("id")))
    }.describe { summary = "Inspect a dead letter" }

    get("dead-letters") {
        val (limit, offset) = call.pageRequest()
        call.respondOk(
            service.listDeadLetters(
                limit,
                offset,
                call.request.queryParameters["eventType"],
                call.request.queryParameters["aggregateType"],
            ),
        )
    }.describe {
        summary = "List dead letters"
        description = "Newest first; optional eventType/aggregateType filters"
    }
}
