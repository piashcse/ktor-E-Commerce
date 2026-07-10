package com.piashcse.mapper

import com.piashcse.database.entities.AuditLogDAO
import com.piashcse.model.response.AuditLogResponse
import java.time.format.DateTimeFormatter

fun AuditLogDAO.toAuditLogResponse() = AuditLogResponse(
    id.value, actorId.value, actorEmail, actorRole, action, resourceType, resourceId,
    details, ipAddress, userAgent, outcome,
    executedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
)
