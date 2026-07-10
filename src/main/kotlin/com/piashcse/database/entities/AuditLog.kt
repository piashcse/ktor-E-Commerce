package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.database.entities.base.currentUtc
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object AuditLogTable : BaseIdTable("audit_log") {
    val actorId = reference("actor_id", UserTable.id).index()
    val actorEmail = varchar("actor_email", 255)
    val actorRole = varchar("actor_role", 50)
    val action = varchar("action", 100).index()
    val resourceType = varchar("resource_type", 100).index()
    val resourceId = varchar("resource_id", 100).nullable()
    val details = text("details").nullable()
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val outcome = varchar("outcome", 20).default("SUCCESS").index()
    val executedAt = datetime("executed_at").clientDefault { currentUtc() }

    init {
        index(customIndexName = "audit_log_executed_at_idx", isUnique = false, executedAt)
    }
}

class AuditLogDAO(id: EntityID<String>) : BaseEntity(id, AuditLogTable) {
    companion object : BaseEntityClass<AuditLogDAO>(AuditLogTable, AuditLogDAO::class.java)
    var actorId by AuditLogTable.actorId
    var actorEmail by AuditLogTable.actorEmail
    var actorRole by AuditLogTable.actorRole
    var action by AuditLogTable.action
    var resourceType by AuditLogTable.resourceType
    var resourceId by AuditLogTable.resourceId
    var details by AuditLogTable.details
    var ipAddress by AuditLogTable.ipAddress
    var userAgent by AuditLogTable.userAgent
    var outcome by AuditLogTable.outcome
    var executedAt by AuditLogTable.executedAt

}
