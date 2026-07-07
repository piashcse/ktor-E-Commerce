package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.model.response.AuditLogResponse
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.format.DateTimeFormatter

object AuditLogTable : BaseIdTable("audit_log") {
    val actorId = reference("actor_id", UserTable.id)
    val actorEmail = varchar("actor_email", 255); val actorRole = varchar("actor_role", 50)
    val action = varchar("action", 100); val resourceType = varchar("resource_type", 100)
    val resourceId = varchar("resource_id", 100).nullable(); val details = text("details").nullable()
    val ipAddress = varchar("ip_address", 45).nullable(); val userAgent = text("user_agent").nullable()
    val outcome = varchar("outcome", 20).default("SUCCESS")
    val executedAt = datetime("executed_at").clientDefault { currentUtc() }
}

class AuditLogDAO(id: EntityID<String>) : BaseEntity(id, AuditLogTable) {
    companion object : BaseEntityClass<AuditLogDAO>(AuditLogTable, AuditLogDAO::class.java)
    var actorId by AuditLogTable.actorId; var actorEmail by AuditLogTable.actorEmail; var actorRole by AuditLogTable.actorRole
    var action by AuditLogTable.action; var resourceType by AuditLogTable.resourceType; var resourceId by AuditLogTable.resourceId
    var details by AuditLogTable.details; var ipAddress by AuditLogTable.ipAddress; var userAgent by AuditLogTable.userAgent
    var outcome by AuditLogTable.outcome; var executedAt by AuditLogTable.executedAt

    fun response() = AuditLogResponse(id.value, actorId.value, actorEmail, actorRole, action, resourceType, resourceId,
        details, ipAddress, userAgent, outcome, executedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
}
