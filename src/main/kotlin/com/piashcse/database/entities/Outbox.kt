package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object OutboxTable : BaseIdTable("outbox") {
    val aggregateType = varchar("aggregate_type", 50).index()
    val aggregateId = varchar("aggregate_id", 50).index()
    val eventType = varchar("event_type", 50).index()
    val payload = text("payload")
    val publishedAt = datetime("published_at").nullable()
    val attempts = integer("attempts").default(0)
}

class OutboxDAO(id: EntityID<String>) : BaseEntity(id, OutboxTable) {
    companion object : BaseEntityClass<OutboxDAO>(OutboxTable, OutboxDAO::class.java)

    var aggregateType by OutboxTable.aggregateType
    var aggregateId by OutboxTable.aggregateId
    var eventType by OutboxTable.eventType
    var payload by OutboxTable.payload
    var publishedAt by OutboxTable.publishedAt
    var attempts by OutboxTable.attempts
}
