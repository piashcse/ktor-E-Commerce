package com.example.entities.user

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object UserTypeTable : IdTable<String>("user_type") {
    override val id: Column<EntityID<String>> = text("user_type_id").uniqueIndex().entityId()
    val user_type = text("user_type")
    override val primaryKey = PrimaryKey(id)
}

class UserTypeEntity(id: EntityID<String>) : Entity<String>(id)  {
    companion object : EntityClass<String, UserTypeEntity>(UserTypeTable)
    var user_type by UserTypeTable.user_type
}
