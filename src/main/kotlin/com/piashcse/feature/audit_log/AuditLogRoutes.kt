@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.feature.audit_log

import com.piashcse.utils.extension.pageRequest
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.requirePathParameter
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.koin.ktor.ext.inject

fun Route.auditLogAdminRoutes() {
    val repo: AuditLogRepository by inject()
    /**
     * Get paginated audit logs with optional filters
     */
    get {
        val (limit, offset) = call.pageRequest()
        call.respondOk(
            repo.getAuditLogs(
                limit,
                offset,
                call.queryParameters["actorId"],
                call.queryParameters["action"],
                call.queryParameters["resourceType"],
                call.queryParameters["resourceId"],
                call.queryParameters["outcome"],
            ),
        )
    }.describe { summary = "Get paginated audit logs with optional filters" }

    /**
     * Get a single audit log entry by ID
     */
    get("{id}") {
        val id = call.requirePathParameter("id")
        call.respondOk(repo.getAuditLogById(id))
    }.describe { summary = "Get a single audit log entry by ID" }
}
