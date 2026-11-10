@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.outbox

import com.piashcse.plugin.ok
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondDeleted
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

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
        call.respondDeleted("Dead letter requeued")
    }.describe {
        summary = "Requeue dead letter"
        description = "Copies the payload back into the outbox"
    }

    get("dead-letters") {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(service.listDeadLetters(limit, offset))
    }.describe {
        summary = "List dead letters"
        description = "Newest first"
    }
}
