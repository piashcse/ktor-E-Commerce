package com.piashcse.plugin

import com.piashcse.event.AdminAudit
import com.piashcse.model.request.JwtTokenRequest
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.CallFailed
import io.ktor.server.application.hooks.ResponseSent
import io.ktor.server.auth.principal
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path

/**
 * Captures successful admin mutations in one place so every admin module gets
 * an audit trail without repeating [EventBus.publishAdminAction] in each route.
 */
val AdminAuditPlugin =
    createApplicationPlugin("AdminAudit") {
        on(ResponseSent) { call ->
            if (call.request.httpMethod == HttpMethod.Get) return@on
            val path = call.request.path()
            if (!path.startsWith("/api/v1/admin/")) return@on
            if (call.response.status()?.isSuccess() != true) return@on
            val principal = call.principal<JwtTokenRequest>() ?: return@on
            val segments = path.removePrefix("/api/v1/admin/").split('/').filter { it.isNotBlank() }
            AdminAudit.publish(
                principal.userId,
                "${call.request.httpMethod.value} ${call.request.path()}",
                segments.firstOrNull() ?: "admin",
                segments.getOrNull(1),
            )
        }
        on(CallFailed) { call, cause ->
            if (call.request.httpMethod == HttpMethod.Get) return@on
            if (!call.request.path().startsWith("/api/v1/admin/")) return@on
            val principal = call.principal<JwtTokenRequest>() ?: return@on
            AdminAudit.publish(
                principal.userId,
                "FAILED ${call.request.httpMethod.value} ${call.request.path()}",
                "admin",
                null,
                cause.message,
            )
        }
    }
