package com.piashcse.feature.audit_log

import com.piashcse.database.entities.AuditLogDAO
import com.piashcse.database.entities.AuditLogTable
import com.piashcse.database.entities.UserTable
import com.piashcse.mapper.toAuditLogResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.extension.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class AuditLogRepositoryImpl : AuditLogRepository {
    override suspend fun log(actorId: String, actorEmail: String, actorRole: String, action: String, resourceType: String,
                             resourceId: String?, details: String?, ipAddress: String?, userAgent: String?, outcome: String) = query {
        AuditLogDAO.new {
            this.actorId = actorId.entityID(UserTable); this.actorEmail = actorEmail; this.actorRole = actorRole
            this.action = action; this.resourceType = resourceType; this.resourceId = resourceId
            this.details = details; this.ipAddress = ipAddress; this.userAgent = userAgent; this.outcome = outcome
        }
        Unit
    }

    override suspend fun getAuditLogs(limit: Int, offset: Int, actorId: String?, action: String?,
                                      resourceType: String?, resourceId: String?, outcome: String?) = query {
        AuditLogTable.selectAll().also { q ->
            listOfNotNull(
                actorId?.let { AuditLogTable.actorId eq it },
                action?.let { AuditLogTable.action eq it },
                resourceType?.let { AuditLogTable.resourceType eq it },
                resourceId?.let { AuditLogTable.resourceId eq it },
                outcome?.let { AuditLogTable.outcome eq it },
            ).forEach { q.andWhere { it } }
            q.orderBy(AuditLogTable.executedAt to SortOrder.DESC)
        }.toPaginatedResponse(limit, offset) { AuditLogDAO.wrapRow(it).toAuditLogResponse() }
    }

    override suspend fun getAuditLogById(logId: String) = query {
        AuditLogDAO.findById(logId)?.toAuditLogResponse() ?: logId.throwNotFound("AuditLog")
    }
}
