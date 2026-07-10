package com.piashcse.feature.audit_log

import com.piashcse.model.response.AuditLogResponse
import com.piashcse.utils.common.PaginatedResponse

class AuditLogService(private val repo: AuditLogRepository) : AuditLogRepository by repo
