package com.piashcse.feature.audit_log

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.piashcse.utils.extension.*
import org.koin.ktor.ext.inject

fun Route.auditLogAdminRoutes() {
    val repo: AuditLogRepository by inject()
    /**
     * @tag AuditLog
     * @description Get paginated audit logs with optional filters
     */
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respondOk(repo.getAuditLogs(limit, offset, call.queryParameters["actorId"], call.queryParameters["action"], call.queryParameters["resourceType"], call.queryParameters["resourceId"], call.queryParameters["outcome"]))
    }

    /**
     * @tag AuditLog
     * @description Get a single audit log entry by ID
     */
    get("{id}") {
        val id = call.pathParameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")
        call.respondOk(repo.getAuditLogById(id))
    }
}
