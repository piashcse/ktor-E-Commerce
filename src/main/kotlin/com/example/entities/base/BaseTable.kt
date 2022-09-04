package com.example.entities.base

import com.example.entities.shop.ShopCategoryTable
import com.example.entities.shop.ShopCategoryTable.uniqueIndex
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

abstract class BaseIntIdTable(name: String) : IdTable<String>(name) {
    override val id: Column<EntityID<String>> = varchar("id", 50).clientDefault { UUID.randomUUID().toString() }.uniqueIndex().entityId()
    val createdAt = datetime("created_at").clientDefault { currentUtc() }
    val updatedAt = datetime("updated_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

abstract class BaseIntEntity(id: EntityID<String>, table: BaseIntIdTable) : Entity<String>(id) {
    val createdAt by table.createdAt
    var updatedAt by table.updatedAt
}
abstract class BaseIntEntityClass<E : BaseIntEntity>(table: BaseIntIdTable) : EntityClass<String, E>(table){
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
fun currentUtc(): LocalDateTime =  LocalDateTime.now(ZoneOffset.UTC)

