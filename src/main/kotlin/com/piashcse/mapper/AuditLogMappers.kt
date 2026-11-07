package com.piashcse.mapper

import com.piashcse.database.entities.AuditLogDAO
import com.piashcse.model.response.AuditLogResponse
import com.piashcse.utils.common.DateTimes.toUtcString

fun AuditLogDAO.toAuditLogResponse() =
    AuditLogResponse(
        id.value, actorId.value, actorEmail, actorRole, action, resourceType, resourceId,
        details, ipAddress, userAgent, outcome,
        executedAt.toUtcString(),
        createdAt.toUtcString(),
    )
