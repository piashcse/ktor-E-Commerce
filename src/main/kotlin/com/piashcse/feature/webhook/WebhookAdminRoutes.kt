@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.webhook

import com.piashcse.constants.WebhookEventStatus
import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

fun Route.webhookAdminRoutes() {
    val service: WebhookAdminService by inject()

    get {
        val (limit, offset) = call.paginateQueryParams()
        val source = call.request.queryParameters["source"]?.parseEnum<WebhookSource>("webhook source")
        val status = call.request.queryParameters["status"]?.parseEnum<WebhookEventStatus>("webhook status")
        call.respondOk(service.list(source, status, limit, offset))
    }.describe {
        summary = "Admin: list payment/carrier webhook events"
        description = "Inspect stored webhook payload status and replay failed/ignored deliveries"
    }

    adminWriteRateLimit {
        post("payments/{id}/replay") {
            call.respondOk(service.replayPayment(call.requirePathParameter("id"), call.currentUserId))
        }.describe { summary = "Admin: replay a stored payment webhook" }

        post("carriers/{id}/replay") {
            call.respondOk(service.replayCarrier(call.requirePathParameter("id"), call.currentUserId))
        }.describe { summary = "Admin: replay a stored carrier webhook" }
    }
}
