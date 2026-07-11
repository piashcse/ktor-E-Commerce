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
        val existingId = call.request.headers[HttpHeaders.XRequestId]
        val requestId = existingId ?: UUID.randomUUID().toString().take(8)
        call.attributes.put(X_REQUEST_ID, requestId)
        MDC.put("requestId", requestId)
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        val requestId = call.attributes[X_REQUEST_ID]
        call.response.header(HttpHeaders.XRequestId, requestId)
    }
}

fun ApplicationCall.requestId(): String = attributes[X_REQUEST_ID]
