package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

/** Terminal outbox rows kept for operator inspection and replay. */
object OutboxDeadLetterTable : BaseIdTable("outbox_dead_letter") {
    val eventType = varchar("event_type", 50).index()
    val aggregateType = varchar("aggregate_type", 50)
    val aggregateId = varchar("aggregate_id", 50)
    val payload = text("payload")
    val error = text("error").nullable()
    val attempts = integer("attempts")
}

class OutboxDeadLetterDAO(id: EntityID<String>) : BaseEntity(id, OutboxDeadLetterTable) {
    companion object : BaseEntityClass<OutboxDeadLetterDAO>(OutboxDeadLetterTable, OutboxDeadLetterDAO::class.java)

    var eventType by OutboxDeadLetterTable.eventType
    var aggregateType by OutboxDeadLetterTable.aggregateType
    var aggregateId by OutboxDeadLetterTable.aggregateId
    var payload by OutboxDeadLetterTable.payload
    var error by OutboxDeadLetterTable.error
    var attempts by OutboxDeadLetterTable.attempts
}
