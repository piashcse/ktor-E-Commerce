package com.piashcse.feature.audit_log

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.piashcse.utils.extension.paginateQueryParams

fun Route.auditLogAdminRoutes(service: AuditLogService) {
    /**
     * @tag AuditLog
     * @description Get paginated audit logs with optional filters
     */
    get {
        val (limit, offset) = call.paginateQueryParams()
        call.respond(
            HttpStatusCode.OK,
            service.getAuditLogs(
                limit, offset,
                call.queryParameters["actorId"], call.queryParameters["action"],
                call.queryParameters["resourceType"], call.queryParameters["resourceId"],
                call.queryParameters["outcome"],
            ),
        )
    }

    /**
     * @tag AuditLog
     * @description Get a single audit log entry by ID
     */
    get("{id}") {
        val id = call.pathParameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")
        call.respond(HttpStatusCode.OK, service.getAuditLogById(id))
    }
}
