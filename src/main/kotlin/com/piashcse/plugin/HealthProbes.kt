package com.piashcse.plugin

import com.piashcse.constants.AppConstants
import com.piashcse.database.isDatabaseReachable
import com.piashcse.utils.common.ApiError
import com.piashcse.utils.common.MessageResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.*
import kotlinx.serialization.Serializable

/** Minimal liveness payload — service/infra detail stays out of the public contract. */
@Serializable
private data class HealthStatusResponse(val status: String, val version: String)

/** `/health`, `/health/live` and `/health/ready` probes. */
@OptIn(ExperimentalKtorApi::class)
internal fun Route.healthRoutes() {
    get("/health") {
        call.respond(
            HealthStatusResponse(status = "UP", version = AppConstants.APP_VERSION),
        )
    }.describe {
        summary = "Service health"
        description = "Version and overall status"
        tag(ApiTags.HEALTH)
    }
    get("/health/live") {
        call.respond(
            MessageResponse("UP"),
        )
    }.describe {
        summary = "Liveness probe"
        tag(ApiTags.HEALTH)
    }
    get("/health/ready") {
        if (isDatabaseReachable()) {
            call.respond(MessageResponse("READY"))
        } else {
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                ApiError.of(
                    message = "Database unreachable",
                    code = "SERVICE_UNAVAILABLE",
                    status = HttpStatusCode.ServiceUnavailable,
                    requestId = runCatching { call.requestId() }.getOrNull(),
                ),
            )
        }
    }.describe {
        summary = "Readiness probe"
        description = "Checks database connectivity"
        tag(ApiTags.HEALTH)
    }
}
