@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.stockalert

import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.plugin.generalRateLimit
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.plugin.writeRateLimit
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondDeleted
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Customer back-in-stock alert subscriptions. */
fun Route.stockAlertRoutes() {
    val service: StockAlertService by inject()
    customerOnlyAuth {
        generalRateLimit {
            get {
                call.respondOk(service.list(call.currentUserId))
            }.describe { summary = "List back-in-stock alerts" }
        }
        writeRateLimit {
            post {
                call.respondCreated(service.subscribe(call.currentUserId, call.receive<StockAlertRequest>()))
            }.describe {
                summary = "Subscribe to a back-in-stock alert"
                requestBody { jsonBody<StockAlertRequest>() }
                responses { ok<StockAlertResponse>("Subscribed") }
            }

            delete("{productId}") {
                val removed = service.unsubscribe(call.currentUserId, call.requirePathParameter("productId"))
                if (!removed) {
                    call.respondOk(com.piashcse.utils.common.MessageResponse("No alert found"))
                } else {
                    call.respondDeleted("Back-in-stock alert removed")
                }
            }.describe { summary = "Remove a back-in-stock alert" }
        }
    }
}
