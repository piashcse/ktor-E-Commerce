package com.piashcse.feature.audit_log

import com.piashcse.model.response.AuditLogResponse
import com.piashcse.utils.common.PaginatedResponse

class AuditLogService(private val auditLogRepo: AuditLogRepository) {
    suspend fun getAuditLogs(
        limit: Int,
        offset: Int,
        actorId: String?,
        action: String?,
        resourceType: String?,
        resourceId: String?,
        outcome: String?,
    ): PaginatedResponse<AuditLogResponse> = auditLogRepo.getAuditLogs(limit, offset, actorId, action, resourceType, resourceId, outcome)

    suspend fun getAuditLogById(logId: String): AuditLogResponse = auditLogRepo.getAuditLogById(logId)
}
