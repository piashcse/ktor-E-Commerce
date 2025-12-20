package com.piashcse.database.entities.base

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityChangeType
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.EntityHook
import org.jetbrains.exposed.v1.dao.toEntity
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

abstract class BaseIdTable(name: String) : IdTable<String>(name) {
    override val id: Column<EntityID<String>> = varchar("id", 50).clientDefault { UUID.randomUUID().toString() }.uniqueIndex().entityId()
    val createdAt = datetime("created_at").clientDefault { currentUtc() }
    val updatedAt = datetime("updated_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

abstract class BaseEntity(id: EntityID<String>, table: BaseIdTable) : Entity<String>(id) {
    val createdAt by table.createdAt
    var updatedAt by table.updatedAt
}

abstract class BaseEntityClass<E : BaseEntity>(table: BaseIdTable, entityType: Class<E>) : EntityClass<String, E>(table, entityType) {
    init {
        EntityHook.subscribe { action ->
            if (action.changeType == EntityChangeType.Updated) {
                try {
                    action.toEntity(this)?.updatedAt = currentUtc()
                } catch (e: Exception) {
                    //nothing much to do here
                }
            }
        }
    }
}

// generating utc time
fun currentUtc(): LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)

