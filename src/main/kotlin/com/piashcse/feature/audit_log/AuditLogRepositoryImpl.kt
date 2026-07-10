package com.piashcse.feature.audit_log

import com.piashcse.database.entities.AuditLogDAO
import com.piashcse.database.entities.AuditLogTable
import com.piashcse.database.entities.UserTable
import com.piashcse.mapper.toAuditLogResponse
import com.piashcse.model.response.AuditLogResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class AuditLogRepositoryImpl : AuditLogRepository {
    override suspend fun log(actorId: String, actorEmail: String, actorRole: String, action: String, resourceType: String,
                             resourceId: String?, details: String?, ipAddress: String?, userAgent: String?, outcome: String) = query {
        AuditLogDAO.new {
            this.actorId = EntityID(actorId, UserTable); this.actorEmail = actorEmail; this.actorRole = actorRole
            this.action = action; this.resourceType = resourceType; this.resourceId = resourceId
            this.details = details; this.ipAddress = ipAddress; this.userAgent = userAgent; this.outcome = outcome
        }
        Unit
    }

    override suspend fun getAuditLogs(limit: Int, offset: Int, actorId: String?, action: String?,
                                      resourceType: String?, resourceId: String?, outcome: String?) = query {
        val q = AuditLogTable.selectAll()
        listOfNotNull(actorId?.let { AuditLogTable.actorId eq it }, action?.let { AuditLogTable.action eq it },
            resourceType?.let { AuditLogTable.resourceType eq it }, resourceId?.let { AuditLogTable.resourceId eq it },
            outcome?.let { AuditLogTable.outcome eq it }).forEach { q.andWhere { it } }

        val count = q.count()
        val data = q.orderBy(AuditLogTable.executedAt to SortOrder.DESC).limit(limit).offset(offset.toLong())
            .map { AuditLogDAO.wrapRow(it).toAuditLogResponse() }
        PaginatedResponse(data, PaginationMetadata(count, limit, offset))
    }

    override suspend fun getAuditLogById(logId: String) = query {
        AuditLogDAO.findById(logId)?.toAuditLogResponse() ?: logId.throwNotFound("AuditLog")
    }
}
