package com.piashcse.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*
import org.slf4j.MDC
import java.util.UUID

val X_REQUEST_ID = AttributeKey<String>("X-Request-ID")

fun Application.installRequestTracing() {
    intercept(ApplicationCallPipeline.Setup) {
        // Fail-safe: blank or malformed inbound IDs are replaced, never propagated or thrown on.
        val existingId =
            call.request.headers[HttpHeaders.XRequestId]?.takeIf { it.isNotBlank() }?.let { raw ->
                runCatching {
                    UUID.fromString(raw)
                    raw
                }.getOrNull()
            }
        val requestId = existingId ?: UUID.randomUUID().toString()
        call.attributes.put(X_REQUEST_ID, requestId)
        MDC.put("requestId", requestId)
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        try {
            val requestId = call.attributes[X_REQUEST_ID]
            call.response.header(HttpHeaders.XRequestId, requestId)
        } finally {
            MDC.remove("requestId")
        }
    }

    intercept(ApplicationCallPipeline.Call) {
        try {
            proceed()
        } finally {
            MDC.remove("requestId")
        }
    }
}

fun ApplicationCall.requestId(): String =
    attributes.getOrNull(X_REQUEST_ID) ?: UUID.randomUUID().toString().also { attributes.put(X_REQUEST_ID, it) }
