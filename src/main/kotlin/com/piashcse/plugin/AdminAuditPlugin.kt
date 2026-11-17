package com.piashcse.plugin

import com.piashcse.event.EventBus
import com.piashcse.model.request.JwtTokenRequest
import io.ktor.http.HttpMethod
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
            val principal = call.principal<JwtTokenRequest>() ?: return@on
            val segments = path.removePrefix("/api/v1/admin/").split('/').filter { it.isNotBlank() }
            EventBus.publishAdminAction(
                Triple(principal.userId, principal.email, principal.getUserType()?.name ?: ""),
                action = "${call.request.httpMethod.value} ${call.request.path()}",
                resourceType = segments.firstOrNull() ?: "admin",
                resourceId = segments.getOrNull(1),
            )
        }
        on(CallFailed) { call, cause ->
            if (call.request.httpMethod == HttpMethod.Get) return@on
            if (!call.request.path().startsWith("/api/v1/admin/")) return@on
            val principal = call.principal<JwtTokenRequest>() ?: return@on
            EventBus.publishAdminAction(
                Triple(principal.userId, principal.email, principal.getUserType()?.name ?: ""),
                action = "FAILED ${call.request.httpMethod.value} ${call.request.path()}",
                resourceType = "admin",
                resourceId = null,
                details = cause.message,
            )
        }
    }
