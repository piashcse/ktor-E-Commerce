@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.promotion

import com.piashcse.plugin.adminWriteRateLimit
import com.piashcse.plugin.created
import com.piashcse.plugin.jsonBody
import com.piashcse.plugin.ok
import com.piashcse.utils.extension.paginateQueryParams
import com.piashcse.utils.extension.respondCreated
import com.piashcse.utils.extension.respondDeleted
import com.piashcse.utils.extension.respondOk
import com.piashcse.utils.validator.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

/** Admin automatic-promotion management. */
fun Route.promotionAdminRoutes() {
    val service: PromotionService by inject()
    get {
        val (limit, offset) = call.paginateQueryParams()
        val active = call.request.queryParameters["active"]?.toBooleanStrictOrNull()
        call.respondOk(service.list(active, limit, offset))
    }.describe { summary = "Admin: list promotions" }

    adminWriteRateLimit {
        post {
            call.respondCreated(service.create(call.receive<PromotionRequest>()))
        }.describe {
            summary = "Admin: create a promotion"
            requestBody { jsonBody<PromotionRequest>() }
            responses { created<PromotionResponse>("Promotion created") }
        }

        put("{id}") {
            call.respondOk(service.update(call.requirePathParameter("id"), call.receive<PromotionRequest>()))
        }.describe {
            summary = "Admin: update a promotion"
            requestBody { jsonBody<PromotionRequest>() }
            responses { ok<PromotionResponse>("Promotion updated") }
        }

        delete("{id}") {
            val id = call.requirePathParameter("id")
            if (!service.delete(id)) throw NotFoundException("Promotion not found")
            call.respondDeleted("Promotion deactivated")
        }.describe { summary = "Admin: deactivate a promotion" }
    }
}
