package com.piashcse.feature.audit_log

import com.piashcse.model.response.AuditLogResponse
import com.piashcse.utils.common.PaginatedResponse

interface AuditLogRepository {
    suspend fun log(actorId: String, actorEmail: String, actorRole: String, action: String, resourceType: String,
                    resourceId: String? = null, details: String? = null, ipAddress: String? = null,
                    userAgent: String? = null, outcome: String = "SUCCESS")
    suspend fun getAuditLogs(limit: Int, offset: Int, actorId: String?, action: String?,
                             resourceType: String?, resourceId: String?, outcome: String?): PaginatedResponse<AuditLogResponse>
    suspend fun getAuditLogById(logId: String): AuditLogResponse
}
