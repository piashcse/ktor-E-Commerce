@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

@file:Suppress("MatchingDeclarationName")

package com.piashcse.feature.webhook

import com.piashcse.constants.WebhookEventStatus
import com.piashcse.model.request.BulkResultResponse
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.jsonBody
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.pageRequest
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class BulkWebhookReplayRequest(
    val source: WebhookSource,
    val ids: List<String>,
)

fun Route.webhookAdminRoutes() {
    val service: WebhookAdminService by inject()

    get {
        val (limit, offset) = call.pageRequest()
        val source = call.request.queryParameters["source"]?.parseEnum<WebhookSource>("webhook source")
        val status = call.request.queryParameters["status"]?.parseEnum<WebhookEventStatus>("webhook status")
        call.respondOk(service.list(source, status, limit, offset))
    }.describe {
        summary = "Admin: list payment/carrier webhook events"
        description = "Inspect stored webhook payload status and replay failed/ignored deliveries"
    }

    get("{id}") {
        val source = call.request.queryParameters["source"]?.parseEnum<WebhookSource>("webhook source")
        call.respondOk(service.get(call.requirePathParameter("id"), source))
    }.describe {
        summary = "Admin: inspect a webhook payload"
        description = "Optional source=PAYMENT|CARRIER"
    }

    adminWriteRateLimit {
        post("payments/{id}/replay") {
            call.respondOk(service.replayPayment(call.requirePathParameter("id"), call.currentUserId))
        }.describe { summary = "Admin: replay a stored payment webhook" }

        post("carriers/{id}/replay") {
            call.respondOk(service.replayCarrier(call.requirePathParameter("id"), call.currentUserId))
        }.describe { summary = "Admin: replay a stored carrier webhook" }
        post("bulk/replay") {
            val body = call.receive<BulkWebhookReplayRequest>()
            val failed =
                body.ids.filter { id ->
                    runCatching {
                        if (body.source == WebhookSource.PAYMENT) {
                            service.replayPayment(id, call.currentUserId)
                        } else {
                            service.replayCarrier(id, call.currentUserId)
                        }
                    }.isFailure
                }
            call.respondOk(BulkResultResponse.of(body.ids.size, failed))
        }.describe {
            summary = "Admin: bulk replay webhooks"
            requestBody { jsonBody<BulkWebhookReplayRequest>() }
        }
    }
}
